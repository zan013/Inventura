# Inventura (Android)

Android nadomestek za obstoječi čitalec elektronske inventure (Zebra + Windows CE,
aplikacija »Moto Scan« podjetja MAOP). Namenjen je Zebra Android terminalom
(npr. TC21/TC26) s skeniranjem prek **DataWedge**. Izvozi `.txt` datoteko v enakem
duhu kot stari čitalec; datoteko prek USB prekopiraš v `C:\temp` in uvoziš v MAOP
inventuro prek gumba **Štetje inventure**.

---

## ⚠️ Eno moraš potrditi: format izvozne datoteke

Stari čitalec je ustvaril datoteko (npr. `06042017_101633742.txt`), ki jo MAOP
uvoz »Štetje inventure« zna prebrati. Da nova aplikacija ostane **100 % združljiva**,
mora biti format vrstice **enak** kot v tej datoteki. **Vzorca te datoteke nimam**,
zato je trenutni format **najboljša ocena**:

```
EAN;KOLIČINA;POPISOVALEC   (ena vrstica na sken, ločilo ;, CRLF)
```

Ko dobiš pravo datoteko iz starega čitalca, odpri
`app/src/main/java/si/lagardere/inventura/export/ExportFormat.kt` in nastavi:

- `order` – vrstni red polj (EAN / KOLIČINA / POPISOVALEC)
- `DELIMITER` – ločilo (`;`, `\t`, `,` …)
- `LINE_ENDING` – konec vrstice (`\r\n` ali `\n`)
- `formatQuantity()` – cela števila ali decimalke

Nič drugega v aplikaciji ni treba spreminjati. **Priporočam: pošlji mi eno staro
`.txt` datoteko in točno nastavim format, da uvoz stoodstotno deluje.**

Znaki: vsebina so samo številke, zato je ASCII/UTF-8 varen. Če bi uvoz zahteval
Windows-1250, spremeni `CHARSET` v `Exporter.kt`.

---

## Kaj aplikacija naredi (preslikava iz starega toka)

| Stari čitalec (CE) | Android aplikacija |
|---|---|
| Meni → Inventura | Glavni zaslon (zajem) |
| Popisovalec (šifra referenta 4–7 mest, ostane zapisan) | Polje **Popisovalec**, se ohrani med skeni in ob ponovnem zagonu |
| EAN prek laserja (rumeni gumb) | Skeniranje prek DataWedge (ali ročni vnos) |
| Količina, privzeto 1 | Polje **Količina**, privzeto 1 |
| Način »samo EAN« (vsak sken = 1) | Stikalo **Skeniraj samo EAN** |
| View records, F2 = izbriši | Zaslon **Zapisi**, gumb **Izbriši** |
| Prenos prek USB v `C:\temp`, po prenosu se čitalec izprazni | **Izvozi** → `.txt` v `Download/Inventura`, po izvozu (opcijsko) izprazni zapise |
| Uvoz »Štetje inventure« v MAOP | Isti postopek – datoteko le prekopiraš v `C:\temp` |

Zapisi so shranjeni v bazi (Room), zato preživijo zaprtje aplikacije ali menjavo
baterije – za razliko od starega čitalca.

---

## Gradnja

Wrapper (`gradlew`, `gradle-wrapper.jar`) je vključen, zato gradnja deluje takoj.

### A) GitHub Actions (najlažje – brez namestitve orodij)
1. Naloži ta projekt v GitHub repozitorij (`main` ali `master`).
2. Workflow `.github/workflows/android.yml` samodejno zgradi APK. Lahko ga sprožiš
   tudi ročno: zavihek **Actions → Build APK → Run workflow**.
3. Ko tek konča, v povzetku pod **Artifacts** prenesi `Inventura-debug-apk`
   (`app-debug.apk`).

### B) Android Studio
1. Namesti Android Studio (Hedgehog ali novejši).
2. `File → Open` → izberi mapo `InventuraAndroid`, počakaj na sinhronizacijo.
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)` → dobiš `app-debug.apk`.

### C) Ukazna vrstica (z nameščenim Android SDK + JDK 17)
```
export ANDROID_HOME=/pot/do/android-sdk
./gradlew assembleDebug
# rezultat: app/build/outputs/apk/debug/app-debug.apk
```

APK nato prenesi na Zebra terminal (USB / MDM / StageNow) in namesti (dovoli
namestitev iz neznanih virov, če ni prek MDM).

Nastavitve: `minSdk 24` (Android 7+), `targetSdk 34`, paket `si.lagardere.inventura`.

---

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
