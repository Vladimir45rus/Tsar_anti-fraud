package com.tsarshield.data.model

enum class LicenseStatus {
    FREE,      // Бесплатный режим
    PAID,      // Оплаченная лицензия
    TRIAL,     // Пробный период
    EXPIRED,   // Лицензия истекла
    SUSPENDED, // Лицензия приостановлена
    DEVICE_MISMATCH, // Несоответствие устройства
    UNKNOWN    // Неизвестный статус
}