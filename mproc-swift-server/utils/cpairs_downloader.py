import yfinance as yf
from forex_python.converter import CurrencyRates
import re

c = CurrencyRates()


fname="currency.sql"
insert_map = {}
i=0
with open(fname, 'r') as file:
    for line in file:
        insert_map[i + 1] = line
        i += 1
    
print(insert_map)

pattern = r"insert into currency\(currency_code, currency_name, is_active\) values \('(\w+)',"

not_supported = ['BTC', 'BYR', 'CUC', 'EEK', 'GQE', 'LTL', 'LVL', 'MRO', 'SCR', 'STD', 'VEB', 'ZWR']
insmap_ids = {}
clist = []
for i, stmt in insert_map.items():
    match = re.search(pattern, stmt)
    if match:
        code = match.group(1)
        insmap_ids[code] = i
        if code not in not_supported:
            clist.append(code)

print(f"insert_map_ids:{insmap_ids}")
exchange_rate_insert_template = "insert into currency_exchange_rate (source_currency_id, target_currency_id, exchange_rate, updated_date) values"

def get_exchange_rate(source, target, timeframe, sid, tid):
    rate_data = yf.Ticker(f"{source}{target}=X").history(period=timeframe)
    if not rate_data.empty:
        rate = rate_data['Close'].iloc[-1] 
        values_str = f"({sid}, {tid}, {rate}, '2024-01-31 00:00:09');"
        print(values_str)
        return values_str


with open('exchange_rate.sql', 'w') as file:
    for currency in currencies:
        current_rates = c.get_rates(currency)
        source_id = insmap_ids[currency]
        for key in current_rates.keys():
            pair = f"{currency}/{key}"
            rate = current_rates[key]
            cspairs[pair] = current_rates[key]
            target_id = insmap_ids[key]
            values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
            print(values_str)
            file.write(f"{exchange_rate_insert_template}{values_str}\n")

currencies_not_supported = []
with open('f_exchange_rate.sql', 'w') as file:
    for c in clist:
        source_id = insmap_ids[c]
        for key in ['USD', 'EUR']:
            rate_data = yf.Ticker(f"{c}{key}=X").history(period="1d")
            if not rate_data.empty:
                rate = rate_data['Close'].iloc[-1]
                print(rate)
                target_id = insmap_ids[key]
                values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
                print(values_str)
                file.write(f"{exchange_rate_insert_template}{values_str}\n")

with open('t_exchange_rate.sql', 'w') as file:
    for c in ['USD', 'EUR']:
        source_id = insmap_ids[c]
        for key in insmap_ids.keys():
            if c is not key:
                rate_data = yf.Ticker(f"{c}{key}=X").history(period="1d")
                if not rate_data.empty:
                    rate = rate_data['Close'].iloc[-1]
                    print(rate)
                    target_id = insmap_ids[key]
                    values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
                    print(values_str)
                    file.write(f"{exchange_rate_insert_template}{values_str}\n")
                else:
                    currencies_not_supported.append(key)

  
"""

pound = 'GBP'
target_id = insmap_ids[pound]
with open('gbp_exchange_rate.sql', 'w') as file:
    for key in not_supported:
        source_id = insmap_ids[key]
        values_str = get_exchange_rate(key, pound, "1d", source_id, target_id)
        file.write(f"{exchange_rate_insert_template}{values_str}\n")

with open('f_exchange_rate.sql', 'w') as file:
    for c in clist:
        source_id = insmap_ids[c]
        for key in ['USD', 'EUR']:
            rate_data = yf.Ticker(f"{c}{key}=X").history(period="1d")
            if not rate_data.empty:
                rate = rate_data['Close'].iloc[-1]
                print(rate)
                target_id = insmap_ids[key]
                values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
                print(values_str)
                file.write(f"{exchange_rate_insert_template}{values_str}\n")

with open('t_exchange_rate.sql', 'w') as file:
    for c in ['USD', 'EUR']:
        source_id = insmap_ids[c]
        for key in insmap_ids.keys():
            if c is not key:
                rate_data = yf.Ticker(f"{c}{key}=X").history(period="1d")
                if not rate_data.empty:
                    rate = rate_data['Close'].iloc[-1]
                    print(rate)
                    target_id = insmap_ids[key]
                    values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
                    print(values_str)
                    file.write(f"{exchange_rate_insert_template}{values_str}\n")
                else:
                    currencies_not_supported.append(key)
print(f"currencies not supported: {currencies_not_supported}")

with open('t_exchange_rate.sql', 'w') as file:
    for c in clist:
        source_id = insmap_ids[c]
        for key in insmap_ids.keys():
            if c is not key:
                rate_data = yf.Ticker(f"{c}{key}=X").history(period="1d")
                if not rate_data.empty:
                    rate = rate_data['Close'].iloc[-1]
                    print(rate)
                    target_id = insmap_ids[key]
                    values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
                    print(values_str)
                    file.write(f"{exchange_rate_insert_template}{values_str}\n")
exchange_rate_insert_template = "insert into currency_exchange_rate (source_currency_id, target_currency_id, exchange_rate, updated_date) values"
with open('exchange_rate.sql', 'w') as file:
    for currency in currencies:
        current_rates = c.get_rates(currency)
        source_id = insmap_ids[currency]
        for key in current_rates.keys():
            pair = f"{currency}/{key}"
            rate = current_rates[key]
            cspairs[pair] = current_rates[key]
            target_id = insmap_ids[key]
            values_str = f"({source_id}, {target_id}, {rate}, '2024-01-31 00:00:09');"
            print(values_str)
            file.write(f"{exchange_rate_insert_template}{values_str}\n")


"""