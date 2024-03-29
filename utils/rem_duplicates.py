import re

pattern = re.compile(r"insert into currency_exchange_rate \(source_currency_id, target_currency_id, exchange_rate, updated_date\) values\((\d+), (\d+), .+?\);", re.IGNORECASE)
unique_pairs = set()

# List to keep unique insert statements
unique_inserts = []
with open('xr.sql', 'r') as file:
    for line in file:
        # Find matches
        match = pattern.search(line)
        if match:
            source_id, target_id = match.groups()
            # check if pair is already seen
            if (source_id, target_id) not in unique_pairs:
                # save pair and insert 
                unique_pairs.add((source_id, target_id))
                unique_inserts.append(line)

# unique_inserts now has all the unique insert statements
# If you want to write these to a new file:
with open('xr_un.sql', 'w') as file:
    for insert in unique_inserts:
        file.write(insert)