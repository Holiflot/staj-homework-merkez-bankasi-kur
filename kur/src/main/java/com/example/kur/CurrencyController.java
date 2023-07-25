package com.example.kur;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@EnableScheduling
public class CurrencyController {

    // Döviz verilerini saklamak için bir liste oluşturuyoruz
    List<CurrencyData> currencyDataList = new ArrayList<>();

    // /api/doviz-bilgileri endpoint'i için GET isteğine karşılık gelen metod
    @GetMapping("/doviz-bilgileri")
    public List<CurrencyData> getDovizBilgileri(@RequestParam(required = false) String kaynak,
                                                @RequestParam(required = false) String tarihSaat,
                                                @RequestParam(required = false) String dovizTipi) {

        // Filtrelenmiş verileri saklamak için bir liste oluşturuyoruz ve başlangıçta tüm verileri ekliyoruz
        List<CurrencyData> filteredData = new ArrayList<>(currencyDataList);

        // Eğer "kaynak" parametresi verilmişse, kaynağa göre verileri filtreliyoruz
        if (kaynak != null) {
            filteredData = filteredData.stream()
                    .filter(data -> data.getCurrencyType().equalsIgnoreCase(kaynak))
                    .collect(Collectors.toList());
        }

        // Eğer "tarihSaat" parametresi verilmişse, tarih ve saat içeren verileri filtreliyoruz
        if (tarihSaat != null) {
            filteredData = filteredData.stream()
                    .filter(data -> data.getLastUpdatedDateTime().contains(tarihSaat))
                    .collect(Collectors.toList());
        }

        // Eğer "dovizTipi" parametresi verilmişse, döviz tipine göre verileri filtreliyoruz
        if (dovizTipi != null) {
            filteredData = filteredData.stream()
                    .filter(data -> data.getCurrencyType().equalsIgnoreCase(dovizTipi))
                    .collect(Collectors.toList());
        }

        // Filtrelenmiş verileri döndürüyoruz
        return filteredData;
    }

    // Her 1 saatte bir çalışacak şekilde zamanlanmış metot
    @Scheduled(fixedRate = 3600000) // her 1 saatte  bir çalışacak
    public void updateCurrencyData() {
        try {
            // TCMB web sitesinden döviz verilerini çekmek için gerekli URL
            String url = "https://www.tcmb.gov.tr/kurlar/today.xml";
            Document doc = Jsoup.connect(url).get();
            Elements currencyList = doc.select("Currency");

            CurrencyData newDolarData = null;
            CurrencyData newEuroData = null;

            // TCMB'den çekilen XML verisini işleyerek yeni döviz verilerini oluşturuyoruz
            for (Element currency : currencyList) {
                String currencyName = currency.select("CurrencyName").text();

                if (currencyName.equals("US DOLLAR")) {
                    newDolarData = new CurrencyData();
                    newDolarData.setCurrencyType(currencyName);
                    newDolarData.setBuyingPrice(currency.select("ForexBuying").first().text());
                    newDolarData.setSellingPrice(currency.select("ForexSelling").first().text());
                } else if (currencyName.equals("EURO")) {
                    newEuroData = new CurrencyData();
                    newEuroData.setCurrencyType(currencyName);
                    newEuroData.setBuyingPrice(currency.select("ForexBuying").first().text());
                    newEuroData.setSellingPrice(currency.select("ForexSelling").first().text());
                }
            }

            // Yeni döviz verilerini güncel tarih ve saat bilgisiyle listeye ekliyoruz
            if (newDolarData != null && newEuroData != null) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                String currentDateAndTime = now.format(formatter);

                newDolarData.setLastUpdatedDateTime(currentDateAndTime);
                newEuroData.setLastUpdatedDateTime(currentDateAndTime);

                // Her güncellemede yeni nesneleri listeye ekliyoruz
                currencyDataList.add(newDolarData);
                currencyDataList.add(newEuroData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
