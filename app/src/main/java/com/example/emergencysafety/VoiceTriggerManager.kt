private fun handleCommand(
    commandKey: String,
    action: () -> Unit
) {
    val now = SystemClock.elapsedRealtime()

    if (
        pendingCommand == commandKey &&
        now - pendingCommandTime <= confirmationWindowMs
    ) {
        pendingCommand = null
        pendingCommandTime = 0L

        Log.d(
            "VoiceTriggerManager",
            "Komut iki kez doğrulandı: $commandKey"
        )

        // İkinci komutta işlem ve titreşim
        action()
        vibrate(longArrayOf(0, 100, 70, 100))
    } else {
        // İlk komutta sadece onay bekle; titreşim verme.
        pendingCommand = commandKey
        pendingCommandTime = now

        Log.d(
            "VoiceTriggerManager",
            "İlk komut alındı; ikinci komut bekleniyor: $commandKey"
        )
    }
}
