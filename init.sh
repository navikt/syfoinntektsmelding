CLIENT_SECRET_PATH=/var/run/secrets/nais.io/azuread/client_secret
if test -f "$CLIENT_SECRET_PATH"; then
  export AAD_SYFOINNTEKTSMELDING_CLIENTID_PASSWORD=$(cat $CLIENT_SECRET_PATH)
fi
