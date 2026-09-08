package com.alkywallet.service;

import com.alkywallet.entity.TipoMoneda;
import java.math.BigDecimal;

public interface ExchangeRateProvider {
    BigDecimal getExchangeRate(TipoMoneda from, TipoMoneda to);
}
