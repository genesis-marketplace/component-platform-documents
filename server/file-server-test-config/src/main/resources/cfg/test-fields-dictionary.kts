fields{

    field("TRADE_DATE", DATE)
    field("TRADE_ID", STRING)
    field(name = "CURRENCY_ID", type = STRING)
    field(name = "INSTRUMENT_ID", type = STRING)
    field(name = "TRADE_TYPE", type = STRING)
    field(name = "QUANTITY", type = INT, nullable = false, default = 0)
    field(name = "PRICE", type = DOUBLE, default = 0.0, nullable = false)
}