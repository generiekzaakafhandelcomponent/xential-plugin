# Release notes

Overzicht van wijzigingen per versie van de Xential-plugin.

## 2.5.0
De callback waarmee Xential gegenereerde documenten aflevert, kan nu worden beveiligd met een gedeeld geheim dat bij de pluginconfiguratie wordt ingesteld. Zolang de verificatie nog niet is afgedwongen worden afwijkende callbacks alleen gelogd, zodat een bestaande koppeling blijft werken totdat het geheim aan beide zijden is ingesteld.

Elke pluginconfiguratie heeft zijn eigen geheim. Een callback wordt gecontroleerd tegen het geheim van de configuratie die het document heeft aangevraagd, zodat meerdere Xential-configuraties naast elkaar kunnen bestaan zonder dat hun geheimen gelijk hoeven te zijn.

Een stortvloed aan onjuist ondertekende callbacks kan geldige callbacks niet meer tegenhouden: alleen callbacks die niet te controleren zijn tellen mee voor de begrenzing.

Let op bij het bijwerken: documentaanvragen die op dat moment al liepen, zijn niet te controleren, omdat daarvan niet is vastgelegd met welke configuratie ze zijn gestart. Zolang de verificatie alleen wordt gelogd, worden die callbacks nog verwerkt met een melding die deze oorzaak benoemt. Laat deze aanvragen eerst aflopen — of herstart de betreffende processen — voordat de verificatie wordt afgedwongen.

## 2.4.2
Casing aangepast van de plugin specificatie referentie.

## 2.4.0
Voorzien van een bouwblok voor het genereren van documenten via Xential templates.

## 1.5.2
Ondergebracht in een eigen repository met voorbeeldapplicatie en aparte documentatie.

## 1.5.1
Documentatie in de plugin-README uitgebreid.

## 1.5.0
Filter op documenttype toegevoegd bij het kiezen van een sjabloon.

## 1.4.2
Ongebruikte TokenAuthenticationService verwijderd.

## 1.4.1
Xential als zelfstandig bouwblok beschikbaar gemaakt en afhandeling van ongeldige Xential-userId verbeterd.

## 1.4.0
Validatiestap toegevoegd, content altijd als tekst behandeld en de Xential-flow vereenvoudigd.

## 1.3.0
Stap "prepare content" hernoemd en interne flow opgeschoond.

## 1.2.3
Kleine correcties in de plugin.

## 1.2.1
Verbeteringen aan het genereren van documenten.

## 1.1.0
Sjabloon en sjabloongroep kunnen nu in een gebruikerstaak gekozen worden.

## 1.0.0
Eerste release: documenten genereren via Xential templates.
