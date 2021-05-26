

<a name="_"></a>
# OBJECT\_SELECTION\_DONE
| Field |Description |Type |
|  ----- | ----- | ----- |
|  | Sendes første gang et objektvalg er foretatt for en forflytning eller forsendelse | object |
| $schema | http://json\-schema.org/draft\-07/schema\# |  |
| title | OBJECT\_SELECTION\_DONE |  |
| metadata | Metadata for denne hendelsen<br />[metadata>](#metadata) | object<br />\[1\]<br />required |
| data | Toppnode<br />[data>](#data) | object<br />\[1\]<br />required |


<a name="metadata"></a>
## metadata
| Field |Description |Type |
|  ----- | ----- | ----- |
| eventId | Unik identifikator for denne hendelsen med UUID v4 format | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required |
| correlationId | Identifikator som brukes for korrelasjon av meldingen på tvers av systemer, samt for produksjon av kvitteringer | required<br />\[string, null\] |
| createdAt | ISO 8601 UTC timestamp for når denne hendelsen ble opprettet, brukes gjerne for debugging | string<br />date\-time<br />required |
| eventType | Hendelsestypen, brukes typisk for forretningslogikk i konsumenter | string<br />required |
| source | Navnet på kilden til hendelsen, typisk applikasjonsnavnet på produsent, brukes gjerne til debugging | string<br />required |
| version | Hendelsesversjonen, brukes typisk for kompatibilitet | string<br />required |


<a name="data"></a>
## data
| Field |Description |Type |
|  ----- | ----- | ----- |
| objectSelection | Detaljer om transporten mellom avsender og mottaker beskrevet i transportkontrakten. Pakke, kolliførste gang et objektvalg er foretatt for en forflytning eller forsendelse<br />[objectSelection>](#data__objectSelection) | object<br />\[0, 1\]<br />required |


<a name="data__objectSelection"></a>
### data > objectSelection
| Field |Description |Type |Eksempel |Info |
|  ----- | ----- | ----- | ----- | ----- |
| id | Unik id på et objektvalg. Ved en oppdatering \(OBJECT\_SELECTION\_CHANGED\) vil denne være lik som i opprinnelig melding \(OBJECT\_SELECTION\_DONE\) | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | 7875daec\-4e3c\-49af\-a58d\-1cc4f0169eed |  |
| processingType | Angir om objektvalget gjelder en forflytning eller en forsendelse. Mulige verdier: MASTER\_CONSIGNMENT og HOUSE\_CONSIGNMENT, som angitt i kodeverket [ObjectSelectionTaskType](https://wikiprod.toll.no/display/KON/Kodeverk+-+Objektutvelgelse#KodeverkObjektutvelgelse-ObjectSelectionTaskType) | string<br />required | HOUSE\_CONSIGNMENT |  |
| objectSelectionType | Angir objektvalget som er gjort. Mulige verdier: NO\_CONTROL, CONTROL, DO\_NOT\_LOAD, som angitt i kodeverket [ObjectSelectionType](https://wikiprod.toll.no/display/KON/Kodeverk+-+Objektutvelgelse#KodeverkObjektutvelgelse-ObjectSelectionType) | string<br />required | NO\_CONTROL |  |
| reasonList | Liste med begrunnelser for objektvalget. Mulige verdier: kodeverket [ObjectSelectionReason](https://wikiprod.toll.no/display/KON/Kodeverk+-+Objektutvelgelse#KodeverkObjektutvelgelse-ObjectSelectionReason)<br />[reasonList>](#data__objectSelection__reasonList) | array<br />\[1, ...\]<br />required | \["NO\_RISK\_FOUND"\] |  |
| reasonDescription | Fritekst som utdyper reason | string |  | optional, ikke med hvis null |
| movementObjectId | Referanse til forsendelsen eller forflytningen som objektvalget gjelder, avhengig av verdi på processingType | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | e9357867\-bb7f\-4ea9\-a5d5\-f571427a42fd |  |
| createdBy | Identifiserer bruker \(person eller maskinell\) som utførte objektvalget | string<br />required | ABCD |  |
| created | Tidspunkt for når objektvalget ble utført. ISO\-8601 format YYYY\-MM\-DDThh:mm:ss\(.\*s\)Z | string<br />date\-time<br />required | 2021\-01\-06T07:18:36.129477500Z |  |
| movementReference | Referanser til opprinnelig melding fra TB med forflytningsdeklarasjonen og/eller forsendelsen som objektvalget gjelder<br />[movementReference>](#data__objectSelection__movementReference) | object<br />required |  |  |
| riskAnalysisResultReferenceList | Array av referanser til opprinnelige meldinger fra RV med automatisk og manuelle risikovurderinger for forflytningsdeklarasjonen og/eller forsendelsen som objektvalget gjelder. Ett objekt pr. regelsett fra RV<br />[riskAnalysisResultReferenceList>](#data__objectSelection__riskAnalysisResultReferenceList) | array<br />required |  |  |


<a name="data__objectSelection__reasonList"></a>
#### data > objectSelection > reasonList
| Field |Description |Type |Eksempel |
|  ----- | ----- | ----- | ----- |
| items |  | string |  |


<a name="data__objectSelection__movementReference"></a>
#### data > objectSelection > movementReference
| Field |Description |Type |Eksempel |
|  ----- | ----- | ----- | ----- |
| declarationId | Identifiserer forflytningsdeklarasjonen, dvs. Declaration.id. Fra opprinnelig melding fra TB | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | 3c166f08\-a210\-46c4\-80a5\-4d81a51f2a8d |
| declarationReference | Felles referanse for alle versjoner av en forflytningsdeklarasjon, dvs. Declaration.declarationReference. Fra opprinnelig melding fra TB | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | g857f532\-1759\-4535\-b04c\-695f2ace3f4c |
| partialProcessingId | Identifiserer delbehandlingen fra TB, dvs. PartialProcessing.id. Fra opprinnelig melding fra TB | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | 4369b7f0\-7371\-4482\-b972\-fc284e0c419c |


<a name="data__objectSelection__riskAnalysisResultReferenceList"></a>
#### data > objectSelection > riskAnalysisResultReferenceList
| Field |Description |Type |Info |Eksempel |
|  ----- | ----- | ----- | ----- | ----- |
| manualRiskAssessmentResultEventId | Identifiserer opprinnelig melding fra OU med manuell risikovurdering for et regelsett | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$ | nullable, sendes ikke ved automatisk bortvalg | 6b94e6f4\-9104\-469f\-8837\-92c121046616 |
| riskAnalysisProcessingId | Identifiserer risikovurderingen fra RV for et regelsett, dvs. RiskAnalysisProcessing.id | string<br />pattern=^\[0\-9a\-f\]\{8\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{4\}\-\[0\-9a\-f\]\{12\}$<br />required | not null | a057f532\-1759\-4535\-b04c\-695f2ace3f4c |