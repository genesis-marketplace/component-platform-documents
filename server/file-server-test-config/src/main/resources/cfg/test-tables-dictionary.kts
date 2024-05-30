tables {
    table("TRADE", 1100) {
        mappingTable(21005, "TL", "FILE_STORAGE")
        TRADE_ID
        TRADE_TYPE
        TRADE_DATE
        INSTRUMENT_ID
        CURRENCY_ID
        QUANTITY
        PRICE
        primaryKey {
            TRADE_ID
        }
        indices {
            unique {
                TRADE_TYPE
                TRADE_ID
            }
            nonUnique {
                CURRENCY_ID
            }
            nonUnique {
                QUANTITY
            }
            nonUnique {
                TRADE_DATE
            }
        }
    }
}