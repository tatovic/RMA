package rs.homeinventory.app.domain.model

// db.md sekcija 2.4 — nikada se ne cuva u bazi, uvek se izvodi iz warrantyExpirationDate po BR-010.
enum class WarrantyStatus {
    AKTIVNA,
    USKORO_ISTICE,
    ISTEKLA,
    NEPOZNATO
}
