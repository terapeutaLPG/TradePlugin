# Nowe funkcje - Historia handlu i poprawki

## Zrealizowane funkcje

### ✅ 1. Automatyczne zamykanie okna handlu

- Okno handlu automatycznie zamyka się po zakończeniu wymiany
- Implementowane w `InventoryListener.completeTrade()` linie 330-331
- Działa dla obu graczy jednocześnie

### ✅ 2. Komenda /wymianahist dla adminów

- **Komenda**: `/wymianahist [gracz]`
- **Uprawnienie**: `tradeplugin.admin` (domyślnie dla operatorów)
- **Funkcje**:
  - Bez argumentu: pokazuje całą historię handlu
  - Z nazwą gracza: pokazuje historię tylko tego gracza
  - GUI podobne do plugin'u InventoryRollback

### ✅ 3. GUI historii handlu

- **Lista handli**: Każdy handel jako książka z informacjami
  - Data i czas wymiany
  - Nazwy graczy uczestniczących
  - Liczba przedmiotów w wymianie
  - Podgląd "Kliknij aby zobaczyć szczegóły"

### ✅ 4. GUI szczegółów handlu

- **Szczegółowy widok wymiany**:
  - Informacje o handlu (data, gracze, ID)
  - Wszystkie przedmioty gracza 1 (lewa strona)
  - Wszystkie przedmioty gracza 2 (prawa strona)
  - Przycisk powrotu do historii
  - Separatory wizualne

### ✅ 5. System zapisu historii

- **Plik**: `plugins/TradePlugin/trade_history.yml`
- **Format**: YAML z serializacją Base64 dla przedmiotów
- **Zapis**: Automatyczny po każdej udanej wymianie
- **Dane**: UUID graczy, nazwy, timestamp, wszystkie przedmioty

### ✅ 6. Automatyczne czyszczenie po 30 dniach

- **Zadanie**: Uruchamiane co 24 godziny
- **Logika**: Usuwa wpisy starsze niż 30 dni
- **Implementacja**: Asynchroniczne, nie blokuje serwera
- **Logs**: Informuje o liczbie usuniętych wpisów

## Struktura kodu

### Nowe klasy:

1. **TradeHistoryManager** - zarządzanie historią
2. **TradeHistoryEntry** - model wpisu historii
3. **TradeHistoryCommand** - komenda /wymianahist
4. **TradeHistoryGUI** - interfejsy GUI historii

### Rozszerzone klasy:

1. **InventoryListener** - obsługa kliknięć w GUI historii
2. **TradePlugin** - integracja managera historii
3. **plugin.yml** - definicja komendy i uprawnień

## Instrukcje użycia

### Dla graczy:

1. Handel działa jak wcześniej, ale okno automatycznie się zamyka
2. Wszyscy handel są zapisywane w historii

### Dla adminów:

```bash
# Zobacz całą historię handlu
/wymianahist

# Zobacz historię konkretnego gracza
/wymianahist nazwa_gracza
```

### W GUI historii:

1. **Lista handli**: Kliknij książkę aby zobaczyć szczegóły
2. **Szczegóły**: Kliknij strzałkę aby wrócić do listy
3. **Zamknij**: Kliknij barierę lub zamknij okno

## Bezpieczeństwo i wydajność

### Bezpieczeństwo:

- Tylko administratorzy mogą przeglądać historię
- Serializacja przedmiotów zabezpieczona try-catch
- Walidacja UUID i dat

### Wydajność:

- Cache w pamięci dla szybkiego dostępu
- Asynchroniczne czyszczenie
- Ograniczenie rozmiaru GUI historii

### Niezawodność:

- Automatyczne tworzenie pliku historii
- Graceful handling błędów deserializacji
- Logs dla debugowania

## Pliki konfiguracyjne

### plugin.yml - nowa komenda:

```yaml
wymianahist:
  description: Zobacz historię handlu (dla adminów)
  usage: /wymianahist [gracz]
  permission: tradeplugin.admin
```

### trade_history.yml - struktura:

```yaml
trades:
  <uuid>:
    timestamp: "2024-01-01T12:00:00"
    player1:
      name: "Gracz1"
      uuid: "uuid-1"
      items: "base64-serialized-items"
    player2:
      name: "Gracz2"
      uuid: "uuid-2"
      items: "base64-serialized-items"
```

## Status implementacji: GOTOWE ✅

Wszystkie żądane funkcje zostały zaimplementowane i przetestowane podczas kompilacji.
