package com.ntros.mprocswift.service.download;

public abstract class AbstractRestDownloadService implements DownloadService {

    private static final String BASE_URL = "https://www.alphavantage.co/query?";
    private static final String FUNCTION = "function=CURRENCY_EXCHANGE_RATE";
    private static final String RESOURCE = "&from_currency=%s&to_currency=%s&apikey=R8HWBA167JXNVQDP";
}
