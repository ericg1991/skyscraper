Cosa devono contenere i file:

ROUTES
Deve contenere una lista di coppie di aeroporti separati da uno spazio, una coppia per ogni riga.
La coppia di aeroporti è formata dall'aeroporto di partenza e dall’aeroporto di ritorno.
Devono essere divisi da uno spazio.

Nota per l'aeroporto: ad ogni aeroporto corrisponde un codice, nel documento si deve inserire il CODICE non il nome per completo dell’aeroporto.
Esempio: volgio fare due diverse interrogazioni:
 - una che parte da Kuala Lumpur International (KUL) e arriva a Nanning (NNG)
 - l'altra che parte da Frankfurt am Main (FRA) e arriva a Berlin (Any)
Il mio file quindi conterrà 2 righe:
KUL NNG
FRA BERL

PASSENGER
Deve contenere una lista di numeri, corrispondenti ai numeri di passeggeri per cui si deve fare la ricerca.
Un numero per ogni riga.

Esempio: voglio fare tre diverse interrogazioni, con 1 passeggero, con 2 e con 3 passeggeri.
Il mio file quindi conterrà 3 righe:
1
2
3

DATE
Deve contenere una lista di coppie di date separate da uno spazio, una coppia per ogni riga.
La coppia di date è formata dalla data di partenza e dalla data di ritorno.
Devono essere separate da uno spazio.

Nota per il formato delle date: ultime due cifre dell’anno, mese e giorno. TUTTO ATTACCATO.
Esempio: voglio fare due interrogazioni: 
 - una con partenza il 25 Luglio 2015 (15 anno 07 mese 25 giorno) e ritorno il 30 Luglio 2015 (15 anno 07 mese 30 giorno)
 - l'altra con partenza sempre il 25 Luglio 2015 (15 anno 07 mese 25 giorno) e ritorno il 28 Luglio 2015 (15 anno 07 mese 28 giorno)
Il file quindi conterrà due righe:
150725 150730
150725 150728

SPECIFICATIONS
Questo file dovrà contenere le specifiche del programma:
 - numero di pagine richieste, 
 - ogni quanto fare le richieste e 
 - quando deve terminare il programma.
Queste tre richieste dovranno essere scritte una per riga: 
 - la prima riga sarà per il numero di pagine, 
 - la seconda riga per la frequenza di richieste e 
 - la terza per il termine delle richieste.

Nota per il termine della ricerca: si deve inserire la data (ANNO – MESE – GIORNO) in cui si vorranno far terminare le richieste.
Il formato dovrà essere: ultime due cifre dell’anno, mese e giorno. TUTTO ATTACCATO.

Esempio: voglio fare le interrogazioni chiedendo 2 pagine, ogni 18000 secondi che termini il 31 Agosto 2015 (15 anno 08 Agosto 31 giorno).
Il file quindi dovrà contenere 3 righe:
2
18000
150831