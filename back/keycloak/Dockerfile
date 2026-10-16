FROM quay.io/keycloak/keycloak:26.2.5

# Copy theme JAR. The "theme" build context is ../keycloak-theme/dist_keycloak (see docker-compose.yml):
# if this step fails with "not found", build the theme first (cd keycloak-theme && pnpm build-keycloak-theme).
COPY --from=theme keycloak-theme-for-kc-all-other-versions.jar /opt/keycloak/providers/keycloak-theme.jar

# Build with dev-friendly settings baked in
ENV KC_HTTP_ENABLED=true
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
# Production-style default. The local docker-compose.yml overrides it with "start-dev --import-realm";
# running "start" also requires KC_HOSTNAME and HTTPS (or a TLS-terminating proxy) to be configured.
CMD ["start", "--optimized", "--import-realm"]
