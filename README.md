# Syfoinntektsmelding

[![Actions Status](https://github.com/navikt/syfoinntektsmelding/workflows/Bygg%20og%20deploy%20til%20prod/badge.svg)](https://github.com/navikt/syfoinntektsmelding/actions)

## Funksjonalitet
Oppgaven til syfoinntektsmelding er å lytte på en kø med nye inntektsmeldinger. Når den finner en ny inntektsmelding 
skal appen opprette eller oppdatere en oppgave i GOSYS for så å ferdigstille en journalpost med oppdatert informasjon.

## Kjøre applikasjonen lokalt
Applikasjonen er avhengig av mange tjenester og det enkleste er å starte den inne fra en tynnklient hvor tjenestene er 
tilgjengelige. 

Applikasjonen må kjøres som en spring-boot applikasjon med local som profil. For å kunne starte så MÅ du sette tre 
environment variabler ellers vil den ikke starte. Det er filen `application-local.properties` som bestemmer 
hvilke tjenster som blir benytter lokalt. For å kunne kjøre må du påse at alle disse miljø innstillingene er riktig
og fylle ut innstillinger for passord da de nå er satt til dummy.

### Påkrevde miljø variabler
```
SECURITYTOKENSERVICE_URL=dummy
SRVSYFOINNTEKTSMELDING_USERNAME=dummy
SRVSYFOINNTEKTSMELDING_PASSWORD=dummy
```

## Diagram
![image](docs/diagram.svg)

## Database
Applikasjonen bruker Postgres database med JPA grensesnitt. Skjermbildet nedenfor viser samtlige tabeller som er brukt.
Flyway blir brukt til versjonering i databasen. Passord mot databasen ligger i vault. 

![image](docs/datamodell.png)



## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver
