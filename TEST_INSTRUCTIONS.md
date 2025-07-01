# Test instrukcje dla TradePlugin

## Jak przetestować plugin:

### 1. Instalacja

1. Skopiuj `TradePlugin.jar` do folderu `plugins/` na serwerze
2. Uruchom serwer ponownie lub użyj `/reload`

### 2. Test podstawowy

1. Zaloguj się dwoma graczami (lub użyj alt konta)
2. Gracz1: `/wymiana Gracz2`
3. Gracz2: Kliknij na `[ZAAKCEPTUJ]` w chacie lub wpisz `/wymianazaakceptuj`
4. Sprawdź czy otwiera się GUI handlu

### 3. Test funkcjonalności

1. Przeciągnij przedmioty z ekwipunku NORMALNIE do slotów handlu (lewa/prawa strona)
2. Sprawdź czy przedmioty się pojavljają u drugiego gracza
3. Kliknij przycisk "Gotowy" (czerwony/zielony blok)
4. Gdy obaj gracze są gotowi, kliknij "Potwierdź handel"
5. Sprawdź czy przedmioty zostały wymienione

### 4. Test anulowania

1. Rozpocznij handel
2. Dodaj przedmioty
3. Kliknij "Anuluj handel" (czerwona bariera)
4. Sprawdź czy przedmioty wróciły do ekwipunku

### 5. Test rozłączenia

1. Rozpocznij handel
2. Jeden gracz rozłącza się
3. Sprawdź czy drugi gracz dostał wiadomość o anulowaniu

## Możliwe problemy i rozwiązania:

### Problem: "Menu nie jest interaktywne"

**Rozwiązanie:**

- Upewnij się, że klikasz w prawne sloty (lewa i prawa strona GUI)
- Nie klikaj w środkowe separatory
- Sprawdź czy masz przedmioty w ekwipunku do przeniesienia

### Problem: "Serwer się wywala"

**Rozwiązanie:**

- Sprawdź logi serwera w folderze `logs/`
- Sprawdź czy masz Java 17+
- Upewnij się, że wersja Spigot jest kompatybilna (1.20+)

### Problem: "Komendy nie działają"

**Rozwiązanie:**

- Sprawdź czy plugin się załadował: `/plugins`
- Sprawdź uprawnienia: `tradeplugin.trade`
- Spróbuj zreloadować plugin: `/reload`

## Debug informacje:

- Plugin loguje wszystkie błędy do konsoli serwera
- Sprawdź plik `logs/latest.log` w przypadku problemów
- Używaj `/wymiana` zamiast `/trade`

## Komendy do testowania:

- `/wymiana <nick>` - rozpocznij handel
- `/wymianazaakceptuj` - zaakceptuj handel (lub kliknij w chat!)
- `/wymianaodrzuc` - odrzuć handel (lub kliknij w chat!)
