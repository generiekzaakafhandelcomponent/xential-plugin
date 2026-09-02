#!/usr/bin/env bash
#
# Generates the X-Xential-Signature header value for a Xential document callback.
#
# The backend (XentialCallbackVerificationService) expects:
#
#     signature = HMAC-SHA256(callbackSecret, documentCreatieSessieId || data)
#
# hex encoded (lowercase), where '||' is concatenation: the HMAC message is the
# documentCreatieSessieId immediately followed by the data field, both as UTF-8
# bytes, with no separator between them.
#
# Usage:
#   generate-xential-signature.sh --secret SECRET --session SESSION_ID --data DATA
#
# Options:
#   -s, --secret     SECRET       The plugin's callbackSecret (shared secret).
#   -i, --session    SESSION_ID   The documentCreatieSessieId.
#   -d, --data       DATA         The base64-encoded data payload.
#   -c, --curl                    Also print a ready-to-run curl command.
#   -x, --execute                 Send the callback with curl (implies --curl).
#   -u, --url        URL          Callback URL for the curl command
#                                 (default: http://localhost:8080/api/v1/xential/document).
#   -h, --help                    Show this help.
#
# Requires: openssl. --execute also requires curl.
#
#

set -euo pipefail

SECRET=""
SESSION_ID=""
DATA=""
EMIT_CURL=false
EXECUTE=false
URL="http://localhost:8080/api/v1/xential/document"

usage() {
    sed -n '2,/^# Requires/p' "$0" | sed 's/^#\{0,1\} \{0,1\}//'
    exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -s|--secret)    SECRET="$2"; shift 2 ;;
        -i|--session)   SESSION_ID="$2"; shift 2 ;;
        -d|--data)      DATA="$2"; shift 2 ;;
        -c|--curl)      EMIT_CURL=true; shift ;;
        -x|--execute)   EXECUTE=true; EMIT_CURL=true; shift ;;
        -u|--url)       URL="$2"; shift 2 ;;
        -h|--help)      usage 0 ;;
        *) echo "Unknown argument: $1" >&2; usage 1 ;;
    esac
done

if [[ -z "$SECRET" ]]; then
    echo "Error: --secret is required." >&2
    exit 1
fi
if [[ -z "$SESSION_ID" ]]; then
    echo "Error: --session is required." >&2
    exit 1
fi
if [[ -z "$DATA" ]]; then
    echo "Error: --data is required." >&2
    exit 1
fi

# The data payload is expected to be base64-encoded already; the server
# base64-decodes 'data', so this encoded string is what must be signed.
#
# Signed material: documentCreatieSessieId concatenated with data, no separator.
# printf '%s' avoids a trailing newline that would corrupt the HMAC input.
SIGNATURE="$(
    printf '%s' "${SESSION_ID}${DATA}" \
        | openssl dgst -sha256 -hmac "$SECRET" -binary \
        | xxd -p -c 256
)"

echo "$SIGNATURE"

if [[ "$EMIT_CURL" == true ]]; then
    # Rebuild the JSON body the endpoint deserializes into DocumentCreatedMessage.
    # Only the fields that matter for verification are filled in with the values used
    # above; the rest are placeholders you can change to suit your test.
    #
    # The exact same DATA that was signed is JSON-encoded here, so the string the
    # server verifies is character-identical to what was signed.
    DATA_JSON="$(printf '%s' "$DATA" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || printf '"%s"' "$DATA")"
    BODY="$(cat <<EOF
{
    "taakapplicatie": "d2dd4dbb-5346-11f0-b9a0-8e2938b31152",
    "gebruiker": "example",
    "documentCreatieSessieId": "$SESSION_ID",
    "formaat": "PDF",
    "documentkenmerk": "example",
    "data": $DATA_JSON
}
EOF
)"

    # Print the equivalent curl command (to stderr, so stdout stays just the signature).
    echo >&2
    echo "curl command:" >&2
    cat >&2 <<EOF
curl -sS -X POST '$URL' \\
  -H 'Content-Type: application/json' \\
  -H 'X-Xential-Signature: $SIGNATURE' \\
  -d '$BODY'
EOF

    if [[ "$EXECUTE" == true ]]; then
        echo >&2
        echo "executing..." >&2

        # Capture the response body and the HTTP status separately: the endpoint
        # always returns an empty body, so the status is what carries the outcome.
        RESPONSE_FILE="$(mktemp)"
        STATUS="$(
            curl -sS -o "$RESPONSE_FILE" -w '%{http_code}' -X POST "$URL" \
                -H 'Content-Type: application/json' \
                -H "X-Xential-Signature: $SIGNATURE" \
                -d "$BODY"
        )"

        echo "HTTP status: $STATUS" >&2
        # Map the status back to the DocumentResource outcome it stands for.
        case "$STATUS" in
            200) echo "  -> processed (signature verified, callback accepted)" >&2 ;;
            404) echo "  -> rejected (bad/missing signature, or unknown/expired session)" >&2 ;;
            429) echo "  -> rate limited" >&2 ;;
            *)   echo "  -> unexpected status" >&2 ;;
        esac

        RESPONSE_BODY="$(cat "$RESPONSE_FILE")"
        rm -f "$RESPONSE_FILE"
        if [[ -n "$RESPONSE_BODY" ]]; then
            echo "Response body:" >&2
            printf '%s\n' "$RESPONSE_BODY" >&2
        else
            echo "Response body: (empty)" >&2
        fi
    fi
fi
