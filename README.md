# TradePlugin

Plugin do handlu między graczami dla serwerów Minecraft Bukkit/Spigot.

## Funkcje

- **Bezpieczny handel** - System handlu 1v1 między graczami
- **GUI handlu** - Intuicyjny interfejs graficzny z naturalnym przeciąganiem przedmiotów
- **System potwierdzeń** - Dwuetapowe potwierdzanie handlu z gotowością i automatycznym countdown'em
- **Automatyczne odliczanie** - Po gotowości obu graczy automatyczna akceptacja po 4 sekundach
- **Klikalne wiadomości** - Akceptacja/odrzucenie handlu przez kliknięcie w chat
- **Automatyczne zwracanie przedmiotów** - W przypadku anulowania handlu
- **Obsługa disconnectów** - Automatyczne anulowanie przy rozłączeniu gracza
- **Zabezpieczenie przed duplikowaniem** - Przedmioty są bezpiecznie przekazywane między graczami
- **Konfigurowalny** - Możliwość dostosowania wiadomości i ustawień

## Komendy

| Komenda              | Opis                              | Uprawnienie         |
| -------------------- | --------------------------------- | ------------------- |
| `/wymiana <gracz>`   | Rozpocznij handel z innym graczem | `tradeplugin.trade` |
| `/wymianazaakceptuj` | Zaakceptuj prośbę o handel        | `tradeplugin.trade` |
| `/wymianaodrzuc`     | Odrzuć prośbę o handel            | `tradeplugin.trade` |

## Uprawnienia

| Uprawnienie         | Opis                                 | Domyślnie |
| ------------------- | ------------------------------------ | --------- |
| `tradeplugin.*`     | Dostęp do wszystkich funkcji pluginu | `true`    |
| `tradeplugin.trade` | Pozwala na handel z innymi graczami  | `true`    |

## Jak używać

1. **Rozpoczęcie handlu**: Użyj komendy `/wymiana <nick_gracza>` aby wysłać prośbę o handel
2. **Akceptacja**: Gracz docelowy może:
   - Kliknąć **[ZAAKCEPTUJ]** lub **[ODRZUĆ]** w wiadomości na czacie
   - Użyć komendy `/wymianazaakceptuj` lub `/wymianaodrzuc`
3. **Dodawanie przedmiotów**: Po otwarciu GUI, przeciągnij przedmioty normalnie do swoich slotów handlu
4. **Potwierdzenie gotowości**: Kliknij przycisk gotowości (czerwony → zielony)
5. **Automatyczne odliczanie**: Gdy obaj gracze są gotowi, rozpoczyna się 4-sekundowe odliczanie
6. **Finalizacja**: Po 4 sekundach handel zostanie automatycznie wykonany
7. **Anulowanie**: W każdej chwili można anulować handel przyciskiem lub zamknięciem GUI

## GUI Handlu

```
┌─────────────────────────────────────────────────────┐
│ [Twoje przedmioty]    │ [Przedmioty drugiego gracza] │
│                       │                             │
│ [Gotowy?]             │                   [Gotowy?] │
│                       │                             │
│        [Anuluj]                                     │
└─────────────────────────────────────────────────────┘
```

### Automatyczne funkcje:

- **Resetowanie gotowości** - Po każdej zmianie przedmiotów
- **4-sekundowy countdown** - Po gotowości obu graczy
- **Automatyczne zakończenie** - Bez konieczności klikania "Potwierdź"
- **Bezpieczna wymiana** - Każdy gracz dostaje tylko przedmioty drugiego gracza
  └─────────────────────────────────────────────────────┘

````

## Instalacja

1. Pobierz plik `TradePlugin.jar`
2. Umieść w folderze `plugins/` na serwerze
3. Uruchom ponownie serwer lub użyj `/reload`
4. Skonfiguruj plugin w `plugins/TradePlugin/config.yml` (opcjonalnie)

## Konfiguracja

Plugin tworzy plik `config.yml` z domyślnymi ustawieniami:

```yaml
settings:
  request-timeout: 60 # Czas na odpowiedź na prośbę (sekundy)
  max-trade-distance: 10.0 # Maksymalna odległość do handlu
  allow-cross-world-trade: false # Handel między światami
  log-trades: true # Logowanie handlu
````

## Budowanie

Plugin używa Maven do budowania:

```bash
mvn clean package
```

Gotowy plik JAR znajdziesz w folderze `target/`.

## Wymagania

- **Serwer**: Bukkit/Spigot/Paper 1.20+
- **Java**: 17+

## Struktura projektu

```
src/
├── main/
│   ├── java/
│   │   └── pl/yourname/tradeplugin/
│   │       ├── TradePlugin.java          # Główna klasa pluginu
│   │       ├── commands/
│   │       │   └── TradeCommand.java     # Obsługa komend
│   │       ├── listeners/
│   │       │   ├── InventoryListener.java # Obsługa GUI
│   │       │   └── PlayerListener.java   # Obsługa graczy
│   │       ├── managers/
│   │       │   └── TradeManager.java     # Manager sesji handlu
│   │       ├── models/
│   │       │   ├── TradeSession.java     # Model sesji handlu
│   │       │   └── TradeRequest.java     # Model prośby o handel
│   │       └── gui/
│   │           └── TradeGUI.java         # Interface handlu
│   └── resources/
│       ├── plugin.yml                    # Konfiguracja pluginu
│       └── config.yml                    # Domyślna konfiguracja
```

## Bezpieczeństwo

- Wszystkie operacje handlu są synchroniczne
- Przedmioty są zwracane przy każdym przerwaniu handlu
- Walidacja wszystkich akcji użytkownika
- Automatyczne czyszczenie przy disconnectach

## Licencja

Ten projekt jest udostępniony na licencji MIT.

## Autor

Stworzony dla społeczności Minecraft.

## Wsparcie

W przypadku problemów lub sugestii, utwórz Issue w repozytorium projektu.
