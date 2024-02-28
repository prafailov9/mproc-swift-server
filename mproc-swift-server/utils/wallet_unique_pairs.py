#!/usr/bin/env python3
import re

def process_sql_file(input_path, output_path):
    unique_pairs = set()
    account_id_counter = 1

    with open(input_path, 'r') as input_file, open(output_path, 'w') as output_file:
        for line in input_file:
            if line.startswith('insert into wallet'):
                account_id, currency_id = re.findall(r'\d+', line)[:2]
                pair = (account_id, currency_id)

                while pair in unique_pairs:
                    account_id = str(account_id_counter)
                    pair = (account_id, currency_id)
                    account_id_counter += 1

                unique_pairs.add(pair)
                new_line = re.sub(r'values \(\d+, \d+,', f'values ({account_id}, {currency_id},', line)
                output_file.write(new_line)
            else:
                output_file.write(line)

# Define your file paths
input_file = 'mysql/data/09_wallet.sql'
output_file = 'mysql/unique_wallets.sql'

# Process the file
process_sql_file(input_file, output_file)
