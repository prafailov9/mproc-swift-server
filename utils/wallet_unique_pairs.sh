#!/bin/bash

# Check if input file path is provided
if [ $# -ne 1 ]; then
    echo "Usage: $0 path/to/your/input.sql"
    exit 1
fi

input_file="$1"
output_file="src/main/resources/sql/data/output.sql"

# Initialize an empty string to keep track of unique pairs
unique_pairs=""

# Variable to generate new unique account_id
account_id_counter=1

# Read each line from the input file
while IFS= read -r line; do
    if [[ $line == insert\ into\ wallet* ]]; then
        # Extract account_id and currency_id
        account_id=$(echo "$line" | awk -F'[(),]' '{print $6}')
        currency_id=$(echo "$line" | awk -F'[(),]' '{print $7}')

        pair="${account_id}-${currency_id}"

        # Check if pair is unique
        if [[ $unique_pairs != *"$pair"* ]]; then
            unique_pairs+="$pair "
        else
            # Find a unique account_id
            while [[ $unique_pairs == *"${account_id_counter}-${currency_id}"* ]]; do
                ((account_id_counter++))
            done
            account_id=$account_id_counter
            unique_pairs+="${account_id}-${currency_id} "

            # Replace account_id in the line
            line=$(echo "$line" | sed "s/values ([^,]*,/values ($account_id,/")
        fi

        echo "$line" >> "$output_file"
    else
        # Write non-matching lines as-is
        echo "$line" >> "$output_file"
    fi
done < "$input_file"

echo "Processed file saved as: $output_file"
