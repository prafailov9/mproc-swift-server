import yfinance as yf
from forex_python.converter import CurrencyRates
import json

c = CurrencyRates()
#currency = 'USD'
#rates = c.get_rates(currency)
#print(f"currency:{currency}, rate_count:{len(rates.keys())}\n available_currencies: {rates.keys()}\n rates: {rates}")


currencies = ['EUR', 'JPY', 'BGN', 'CZK', 'DKK', 'GBP', 'HUF', 'PLN', 'RON', 'SEK', 'CHF', 'ISK', 'NOK', 'TRY', 'AUD', 'BRL', 'CAD', 'CNY', 'HKD', 'IDR', 'INR', 'KRW', 'MXN', 'MYR', 'NZD', 'PHP', 'SGD', 'THB', 'ZAR']
cmap = {}
cspairs = {}
for curr in currencies:
    current_rates = c.get_rates(curr)
    print(f"{curr} pairs")
    cmap[curr] = current_rates
    print(current_rates)
    for k in current_rates.keys():
        cspairs[f"{curr}/{k}"] = current_rates[k]

with open('cspairs.json', 'w') as file:
    json.dump(cspairs, file, indent=4)



cpairs = {}

for key in cmap.keys():
    print(f"currency: {key}")
    print(f"{cmap[key]}")
    for pair in cmap[key]:
        cpairs[f"{key}/{pair}"] = cmap[key][pair]
        
print(cpairs)

fname="cmap.json"
with open(fname, 'w') as file:
    json.dump(cmap, file, indent=4)

filename="cpairs.json"

with open(filename, 'w') as file:
    json.dump(cpairs, file, indent=4)

