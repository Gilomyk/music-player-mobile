# Lokalny Odtwarzacz Muzyki 🎵

To jest lokalna aplikacja mobilna na Androida napisana w Javie, służąca do przeglądania i odtwarzania muzyki z pamięci urządzenia.

## Funkcje
- Przeglądanie i odtwarzanie plików .mp3 z pamięci urządzenia
- Tworzenie, edycja i przeglądanie playlist
- Tryb losowego odtwarzania (Shuffle)
- Tryb kolejki (Queue)
- Możliwość wyboru utworów do playlist
- Przewijanie i pauzowanie muzyki
- Automatyczne odświeżanie playlisty

## W planach 🚧
- Historia ostatnich odtworzeń
- Obsługa wyjątków (np. zmiana urządzenia audio)
- Lepszy interfejs graficzny (UI)
- Equalizer i analiza dźwięku
- Inteligentne sterowanie odtwarzaniem

## Wymagania
- Android Studio (rekomendowana wersja 2023.1+)
- Java 8+
- Uprawnienia do czytania pamięci urządzenia

## MVVM refaktor (MainActivity)
- UI wiring i obsługa widoków: `ui/main/MainActivity.kt`
- Stan i intencje użytkownika: `ui/main/MainViewModel.kt`
- Logika odtwarzania: `domain/player/PlaybackController.kt` + `data/player/AudioPlayerImpl.kt`
- Playlisty: `manager/PlaylistManager.kt` + `data/playlist/PlaylistRepositoryImpl.kt`

### Uruchomienie/testy
- Otwórz projekt w Android Studio i uruchom aplikację na urządzeniu/emulatorze.
- Testy jednostkowe: `./gradlew test`

## Autor
Łukasz Chmielnicki
