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
# Example ./generate-xential-signature.sh --secret local-development-secret --session "6d4b2fa2-5d02-4949-ace3-53c9b0ff1e4e" -d "JVBERi0xLjUKJeLjz9MKNCAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvTGVuZ3RoIDkxOT4+c3RyZWFtCniclZddb9MwFIbv/SvMHQjNOHb8kd0BYxJIICYVcYG46NZs67akW5oy7d9zkvjzNApiW9ee1O/j8x77uOkT+bAinNqyYNbS1YZ8WpEL8kQ449Io+kwE/QLv35GC06/k129ON0RqarkaxjdEqYoJ6+MHH0vNeAkhDE1ehlG35CdpYY5Kwg/Fz90N0ZrqomJVBVPAa1UJVikQCrgINBy7sQP4Gl1rjjVWMFMcxTkju9YcacpCsxKHGSG71GCBEmOZcJwROLPUP4aSKKYl1YIzqOTkqix8PFTCMlc1XUCsNZM2hr4wURIQQxLKRoSqJINVOEYmDKdpXFYBMc2KCSEeCJmiyW0dpY14UwZTVsGFlYxXSSFgqWQZEd515iLTBEasxMSIPhAzYQQjwWuopqtUzpjzghnJoua5Y6bfqkNu3suw04yNjCHmMmE476mXXBMYwYtjhHkxM2FEL95vqKmrV86Y84IZydrmuWOmz4MnjTK0llaRoYRlCcJbT63kkoAIViZEmBURE0IwEswmBTUWI+Z8YES2rmniGJn22nipOd6SaMv6bnNh1u8IAWR3AE8IkJoZYoqYJOHI8AQ3KQK4MDGB5N4DyjmnZT3mHaA9c7RP0amY9TpihCo4hjeBkCnC2whGVfwkG6uUI2aMYEJcTXycZ8Ssv5wPvGfwNsXnYtbniOGNeIabFiNTRHDizYZyulrliBkjmBAXFR/oGTHrL+cjtCxqSdyxiY9cEhHeh0O4SREwBXgbwWlSS2MxYcYEBqQLig9yk/cHZ6XW2lD8DLcccGv47rygFV1dk4Jy+C2ogByKghptmCroqiGvP7d/dturmt7Ubd2t+3pDn7f9LT3v6rpZd/d192Z1N9xWciaof3i4oKJM6bCfjdBM6JTs9eOYqCwEUmpwXahR+fFhW7f9qVM6H7mg4FAjlYro47buU8ncHFDpshyHn4HXxRmqbDgVXKgTrk+EXXF+Ov695QL+/2tKaZgRWam/HZrLulu2p+EWXqViqkopluYyYrz34oLxqf5n9f6q2z722127pBNCjXcpUXjRvywKbDV+jEfBj3bb0+9dXOxZXSnE+JEXde+b3aHtl+owmVIGUhwVG/C0NN6ZiYJycfTkJI42S6Nd/glbLto1kgk4HeC7mZx26GrXrx9OqVQ69NT/Na+smAUinAJWTsTbdXtPX3YHer3rhueOXh7227be71+5OYavgxfkL6Pb5icKZW5kc3RyZWFtCmVuZG9iagoxIDAgb2JqCjw8L0NvbnRlbnRzIDQgMCBSL1R5cGUvUGFnZS9SZXNvdXJjZXM8PC9Gb250PDwvRjEgMiAwIFIvRjIgMyAwIFI+Pj4+L1BhcmVudCA1IDAgUi9NZWRpYUJveFswIDAgNTk1LjI4IDg0MS44OF0+PgplbmRvYmoKNyAwIG9iago8PC9EZXN0WzEgMCBSL1hZWiAwIDc2NS40NSAwXS9UaXRsZShJbnZvaWNlKS9QYXJlbnQgNiAwIFI+PgplbmRvYmoKNiAwIG9iago8PC9UeXBlL091dGxpbmVzL0NvdW50IDEvRmlyc3QgNyAwIFIvTGFzdCA3IDAgUj4+CmVuZG9iagoyIDAgb2JqCjw8L1N1YnR5cGUvVHlwZTEvVHlwZS9Gb250L0Jhc2VGb250L0hlbHZldGljYS9FbmNvZGluZy9XaW5BbnNpRW5jb2Rpbmc+PgplbmRvYmoKMyAwIG9iago8PC9TdWJ0eXBlL1R5cGUxL1R5cGUvRm9udC9CYXNlRm9udC9IZWx2ZXRpY2EtQm9sZC9FbmNvZGluZy9XaW5BbnNpRW5jb2Rpbmc+PgplbmRvYmoKNSAwIG9iago8PC9LaWRzWzEgMCBSXS9UeXBlL1BhZ2VzL0NvdW50IDE+PgplbmRvYmoKOCAwIG9iago8PC9UeXBlL0NhdGFsb2cvT3V0bGluZXMgNiAwIFIvUGFnZXMgNSAwIFI+PgplbmRvYmoKOSAwIG9iago8PC9DcmVhdGlvbkRhdGUoRDoyMDI1MDYyNzExMzc1M1opL1Byb2R1Y2VyKE9wZW5QREYgMi4wLjMpL1RpdGxlKEludm9pY2UpPj4KZW5kb2JqCnhyZWYKMCAxMAowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDEwMDEgMDAwMDAgbiAKMDAwMDAwMTI2OCAwMDAwMCBuIAowMDAwMDAxMzU2IDAwMDAwIG4gCjAwMDAwMDAwMTUgMDAwMDAgbiAKMDAwMDAwMTQ0OSAwMDAwMCBuIAowMDAwMDAxMjAzIDAwMDAwIG4gCjAwMDAwMDExMjggMDAwMDAgbiAKMDAwMDAwMTUwMCAwMDAwMCBuIAowMDAwMDAxNTYwIDAwMDAwIG4gCnRyYWlsZXIKPDwvSW5mbyA5IDAgUi9JRCBbPDY0ZjhjNGY2ZWIwZGZmNDE4NTNhZGNmZTFlY2Y2NWNlPjw2NGY4YzRmNmViMGRmZjQxODUzYWRjZmUxZWNmNjVjZT5dL1Jvb3QgOCAwIFIvU2l6ZSAxMD4+CnN0YXJ0eHJlZgoxNjUxCiUlRU9GCg==" -x
#

set -euo pipefail

SECRET=""
SESSION_ID=""
DEFAULT_DATA="JVBERi0xLjUKJeLjz9MKNCAwIG9iago8PC9GaWx0ZXIvRmxhdGVEZWNvZGUvTGVuZ3RoIDkxOT4+c3RyZWFtCniclZddb9MwFIbv/SvMHQjNOHb8kd0BYxJIICYVcYG46NZs67akW5oy7d9zkvjzNApiW9ee1O/j8x77uOkT+bAinNqyYNbS1YZ8WpEL8kQ449Io+kwE/QLv35GC06/k129ON0RqarkaxjdEqYoJ6+MHH0vNeAkhDE1ehlG35CdpYY5Kwg/Fz90N0ZrqomJVBVPAa1UJVikQCrgINBy7sQP4Gl1rjjVWMFMcxTkju9YcacpCsxKHGSG71GCBEmOZcJwROLPUP4aSKKYl1YIzqOTkqix8PFTCMlc1XUCsNZM2hr4wURIQQxLKRoSqJINVOEYmDKdpXFYBMc2KCSEeCJmiyW0dpY14UwZTVsGFlYxXSSFgqWQZEd515iLTBEasxMSIPhAzYQQjwWuopqtUzpjzghnJoua5Y6bfqkNu3suw04yNjCHmMmE476mXXBMYwYtjhHkxM2FEL95vqKmrV86Y84IZydrmuWOmz4MnjTK0llaRoYRlCcJbT63kkoAIViZEmBURE0IwEswmBTUWI+Z8YES2rmniGJn22nipOd6SaMv6bnNh1u8IAWR3AE8IkJoZYoqYJOHI8AQ3KQK4MDGB5N4DyjmnZT3mHaA9c7RP0amY9TpihCo4hjeBkCnC2whGVfwkG6uUI2aMYEJcTXycZ8Ssv5wPvGfwNsXnYtbniOGNeIabFiNTRHDizYZyulrliBkjmBAXFR/oGTHrL+cjtCxqSdyxiY9cEhHeh0O4SREwBXgbwWlSS2MxYcYEBqQLig9yk/cHZ6XW2lD8DLcccGv47rygFV1dk4Jy+C2ogByKghptmCroqiGvP7d/dturmt7Ubd2t+3pDn7f9LT3v6rpZd/d192Z1N9xWciaof3i4oKJM6bCfjdBM6JTs9eOYqCwEUmpwXahR+fFhW7f9qVM6H7mg4FAjlYro47buU8ncHFDpshyHn4HXxRmqbDgVXKgTrk+EXXF+Ov695QL+/2tKaZgRWam/HZrLulu2p+EWXqViqkopluYyYrz34oLxqf5n9f6q2z722127pBNCjXcpUXjRvywKbDV+jEfBj3bb0+9dXOxZXSnE+JEXde+b3aHtl+owmVIGUhwVG/C0NN6ZiYJycfTkJI42S6Nd/glbLto1kgk4HeC7mZx26GrXrx9OqVQ69NT/Na+smAUinAJWTsTbdXtPX3YHer3rhueOXh7227be71+5OYavgxfkL6Pb5icKZW5kc3RyZWFtCmVuZG9iagoxIDAgb2JqCjw8L0NvbnRlbnRzIDQgMCBSL1R5cGUvUGFnZS9SZXNvdXJjZXM8PC9Gb250PDwvRjEgMiAwIFIvRjIgMyAwIFI+Pj4+L1BhcmVudCA1IDAgUi9NZWRpYUJveFswIDAgNTk1LjI4IDg0MS44OF0+PgplbmRvYmoKNyAwIG9iago8PC9EZXN0WzEgMCBSL1hZWiAwIDc2NS40NSAwXS9UaXRsZShJbnZvaWNlKS9QYXJlbnQgNiAwIFI+PgplbmRvYmoKNiAwIG9iago8PC9UeXBlL091dGxpbmVzL0NvdW50IDEvRmlyc3QgNyAwIFIvTGFzdCA3IDAgUj4+CmVuZG9iagoyIDAgb2JqCjw8L1N1YnR5cGUvVHlwZTEvVHlwZS9Gb250L0Jhc2VGb250L0hlbHZldGljYS9FbmNvZGluZy9XaW5BbnNpRW5jb2Rpbmc+PgplbmRvYmoKMyAwIG9iago8PC9TdWJ0eXBlL1R5cGUxL1R5cGUvRm9udC9CYXNlRm9udC9IZWx2ZXRpY2EtQm9sZC9FbmNvZGluZy9XaW5BbnNpRW5jb2Rpbmc+PgplbmRvYmoKNSAwIG9iago8PC9LaWRzWzEgMCBSXS9UeXBlL1BhZ2VzL0NvdW50IDE+PgplbmRvYmoKOCAwIG9iago8PC9UeXBlL0NhdGFsb2cvT3V0bGluZXMgNiAwIFIvUGFnZXMgNSAwIFI+PgplbmRvYmoKOSAwIG9iago8PC9DcmVhdGlvbkRhdGUoRDoyMDI1MDYyNzExMzc1M1opL1Byb2R1Y2VyKE9wZW5QREYgMi4wLjMpL1RpdGxlKEludm9pY2UpPj4KZW5kb2JqCnhyZWYKMCAxMAowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDEwMDEgMDAwMDAgbiAKMDAwMDAwMTI2OCAwMDAwMCBuIAowMDAwMDAxMzU2IDAwMDAwIG4gCjAwMDAwMDAwMTUgMDAwMDAgbiAKMDAwMDAwMTQ0OSAwMDAwMCBuIAowMDAwMDAxMjAzIDAwMDAwIG4gCjAwMDAwMDExMjggMDAwMDAgbiAKMDAwMDAwMTUwMCAwMDAwMCBuIAowMDAwMDAxNTYwIDAwMDAwIG4gCnRyYWlsZXIKPDwvSW5mbyA5IDAgUi9JRCBbPDY0ZjhjNGY2ZWIwZGZmNDE4NTNhZGNmZTFlY2Y2NWNlPjw2NGY4YzRmNmViMGRmZjQxODUzYWRjZmUxZWNmNjVjZT5dL1Jvb3QgOCAwIFIvU2l6ZSAxMD4+CnN0YXJ0eHJlZgoxNjUxCiUlRU9GCg=="
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

# Fall back to the bundled sample PDF payload when no --data was supplied.
if [[ -z "$DATA" ]]; then
    DATA="$DEFAULT_DATA"
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
