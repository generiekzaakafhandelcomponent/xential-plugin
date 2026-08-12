# Getting Started

## Prerequisites

- Java 21
- Node.js >= 20
- Docker & Docker Compose

## Running the application

All commands below should be run from the **project root** directory.

### 1. Configure environment

Copy `.env.properties.example` to `.env.properties` and fill in the values. Every variable in that file is
mandatory: the plugin configurations under `backend/app/src/main/resources/config/plugin` are deployed on every
startup, and a variable that cannot be resolved aborts startup with
`Failed to find environment variable: '<NAME>'`. Check the file again after pulling changes, in case a new
variable was added.

The example file already carries usable local values for everything except the three `MTLS_SSLCTX_*` variables.
Those hold the client certificate the application presents to the ESB, and the mTLS SSLContext plugin marks them
as required, so a blank value aborts startup as well. The ESB is not reachable from a local machine, so a
throwaway self-signed pair is enough:

```shell
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 30 -nodes -subj "/CN=localhost"
base64 -i cert.pem   # MTLS_SSLCTX_SERVER_CERT_BASE64 and MTLS_SSLCTX_CLIENT_CERT_BASE64
base64 -i key.pem    # MTLS_SSLCTX_PRIVATE_KEY_BASE64
```

Paste each value as a single line. `.env.properties` is gitignored; keep it that way.

### 2. Start Docker dependencies

Make sure Docker is running, then start the required services. First set the
Compose profile, then start the containers:

```shell
export COMPOSE_PROFILES=zgw
docker compose -f backend/app/docker-compose.yml up -d
```

### 3. Start the backend

```shell
./gradlew :backend:app:bootRun
```

### 4. Start the frontend

```shell
cd frontend
npm install
npm run libs-build-all
npm start
```

### Autodeployed plugin configurations

The configurations in `backend/app/src/main/resources/config/plugin` are redeployed on every startup, and
deployment replaces the whole property set. Editing one of them in the management interface therefore only lasts
until the next restart: anything the file does not mention is gone, and anything it does mention is reset to the
value from `.env.properties`. That applies to `callbackSecret` too, which is why it is in the file rather than
left to be filled in by hand.

To try out a configuration that is managed from the management interface, create a second one there instead of
editing the autodeployed one.

### Keycloak users

The application has a few test users that are preconfigured.

| Name         | Role           | Username  | Password  |
|--------------|----------------|-----------|-----------|
| James Vance  | ROLE_USER      | user      | user      |
| Asha Miller  | ROLE_ADMIN     | admin     | admin     |
| Morgan Finch | ROLE_DEVELOPER | developer | developer |

## Plugin development

The plugin source code is located in:
- Backend: `backend/plugin/src/`
- Frontend: `frontend/projects/plugin/src/`

For more information on how to build a plugin, see
the [Custom Plugin Definition](https://docs.valtimo.nl/features/plugins/plugins/custom-plugin-definition) documentation.
