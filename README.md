# Inventura (Android)

Android nadomestek za obstoječi čitalec elektronske inventure (Zebra + Windows CE,
aplikacija »Moto Scan«). Namenjen je Zebra Android terminalom
(npr. TC21/TC26) s skeniranjem prek **DataWedge**. Izvozi `.txt` datoteko v enakem
duhu kot stari čitalec; datoteko prek USB prekopiraš v `C:\temp` in uvoziš v MAOP
inventuro prek gumba **Štetje inventure**.



## DataWedge (skener)

Aplikacija ob **prvem zagonu samodejno ustvari DataWedge profil** »Inventura«,
vezan na to aplikacijo: vklopi skener in pošilja skenirano kodo kot broadcast
(`si.lagardere.inventura.SCAN`), tipkovni izpis (keystroke) pa izklopi. Ročno
nastavljanje na vsaki napravi torej ni potrebno. Nastavitev lahko kadarkoli sprožiš
ponovno prek menija **Nastavi skener**.

**Ročna nastavitev (če samodejna ne uspe):** DataWedge → nov profil → poveži z
aplikacijo Inventura → Barcode Input: On → Intent Output: On, Action
`si.lagardere.inventura.SCAN`, Delivery = Broadcast → Keystroke Output: Off.

---

## Uporaba

1. Vnesi **Popisovalca** (šifra referenta) – ostane, dokler ga ne pretipkaš.
2. Skeniraj EAN. Če je »samo EAN« izklopljen, popravi **Količino** in pritisni Enter
   (ali gumb Shrani); privzeto je 1. Ob shranjevanju kratek pisk in vibracija.
3. Za pregled/brisanje odpri **Zapisi** (meni).
4. Ob koncu **Izvozi (prenos)** → vpiši šifro skladišča (neobvezno) → datoteka se
   shrani v `Download/Inventura/DDMMYYYY_HHMMSSmmm[_skladišče].txt`. Po uspešnem
   izvozu se zapisi (opcijsko) izpraznijo.
5. Terminal priklopi na PC prek USB, datoteko iz `Download/Inventura` prekopiraj v
   `C:\temp`, nato v MAOP inventuri klikni **Štetje inventure** in izberi datoteko.

---

## Struktura

```
app/src/main/java/si/lagardere/inventura/
  MainActivity.kt        – zaslon za zajem (skeniranje, shranjevanje)
  RecordsActivity.kt     – pregled in brisanje zapisov
  RecordAdapter.kt       – seznam zapisov
  data/                  – Room: Record, RecordDao, AppDatabase
  export/ExportFormat.kt – ★ format izvozne vrstice (tu prilagodiš)
  export/Exporter.kt     – zapis datoteke (ime kot stari čitalec)
  scan/ScanReceiver.kt   – sprejem skena iz DataWedge
  scan/DataWedgeHelper.kt– samodejna nastavitev DataWedge profila
  util/Prefs.kt          – shranjene nastavitve (popisovalec, skladišče …)
```
