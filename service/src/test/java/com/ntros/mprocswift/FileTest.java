package com.ntros.mprocswift;

import com.ntros.mprocswift.exceptions.AddressHashingFailedException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Paths.get;
import static java.security.MessageDigest.getInstance;

/**
 * Not an actual test. Just "scripts" to fix the generated sql insert statements.
 */
public class FileTest {

    private static final String TEST_FILES_DIR = "src/test/java/com/ntros/mprocswift/test";
    private static final Long ACCOUNT_ORIGIN = 10000000L; // 8 digits
    private static final Long ACCOUNT_BOUND = 9999999999L; // 10 digits
    private static final Long ROUTING_ORIGIN = 100000000L; // 9 digits
    private static final Long ROUTING_BOUND = 999999999L;


    @Test
    public void setOneMainWalletForEachAccountTest() throws IOException {
        // name of module
//        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        Path rootPath = get(TEST_FILES_DIR);
        Path source = rootPath.resolve("source_wallet.sql");
        BufferedReader reader = newBufferedReader(source);
        List<String> inserts = new ArrayList<>();
        Set<String> accounts = new HashSet<>();

        reader.lines().filter(line -> line.startsWith("insert"))
                .forEach(line -> {

                    // capture values part
                    String values = line.substring(line.lastIndexOf('('));
                    String cleanValues = values.substring(1, values.length() - 2);
                    String[] valArr = cleanValues.split(",");
                    String accountId = valArr[0];

                    String substring = values.substring(values.lastIndexOf(',') + 2, values.length() - 2);
                    if (accounts.contains(accountId)) {
                        String newLine = line.replace(substring, "false");
                        inserts.add(newLine);
                    } else {
                        String newLine = line.replace(substring, "true");
                        inserts.add(newLine);
                        accounts.add(accountId);
                    }
                });

        System.out.println(inserts);

        // modify insert statements with new data
        Path dest = rootPath.resolve("dest_wallet.sql");
        BufferedWriter writer = newBufferedWriter(dest);
        for (String line : inserts) {
            System.out.println("writing line: \n" + line);
            writer.write(line + "\n");
        }
        writer.close();
    }


    /**
     * should have unique 8-12 digit account_numbers and 9 digit routing numbers
     * insert into account_details (account_name, account_number, routing_number, iban, bicswift, bank_address)
     * values ('Bank of America Corporation', 14, 9, 'CR27 4617 0625 5742 6037 6', '2W30X3Z', '507 Barby Park');
     */
    @Test
    public void fixAccountDataTest() throws IOException {
        Path rootPath = get(TEST_FILES_DIR);
        Path source = rootPath.resolve("dest_account_details.sql");

        BufferedReader reader = newBufferedReader(source);
        Random rng = new Random();

        // init sets
        Set<String> accountNumbers = new HashSet<>();
        Set<String> routingNumbers = new HashSet<>();

        accountNumbers.add(String.valueOf(rng.nextLong(ACCOUNT_BOUND - ACCOUNT_ORIGIN) + ACCOUNT_ORIGIN));
        routingNumbers.add(String.valueOf(rng.nextLong(ROUTING_BOUND - ROUTING_ORIGIN) + ROUTING_ORIGIN));

        List<String> fixedInserts = new ArrayList<>();
        reader.lines()
                .filter(line -> line.startsWith("insert"))
                .forEach(line -> {
                    // get and clean up values string
                    String valuesString = line.substring(line.lastIndexOf('('));

                    // get only the numbers
                    List<String> numberValues = Stream.of(valuesString.split(", "))
                            .filter(value -> value.matches("[0-9]+")) // checks if the line contains only numbers
                            .toList();

                    // generate unique account and routing numbers
                    String account = String.valueOf(rng.nextLong(ACCOUNT_BOUND - ACCOUNT_ORIGIN) + ACCOUNT_ORIGIN);
                    String routing = String.valueOf(rng.nextLong(ROUTING_BOUND - ROUTING_ORIGIN) + ROUTING_ORIGIN);
                    while (accountNumbers.contains(account)) {
                        account = String.valueOf(rng.nextLong(ACCOUNT_BOUND - ACCOUNT_ORIGIN) + ACCOUNT_ORIGIN);
                    }
                    accountNumbers.add(account);

                    while (routingNumbers.contains(routing)) {
                        routing = String.valueOf(rng.nextLong(ROUTING_BOUND - ROUTING_ORIGIN) + ROUTING_ORIGIN);
                    }
                    routingNumbers.add(routing);
                    System.out.printf("Generated acc_no: %s\nGenerated routing_no: %s%n", account, routing);

                    // using last digit of the account number as starting point since the routing number is
                    // located right after account number in the values string
                    valuesString = valuesString.replace("', " + numberValues.get(0), "', '" + account + "'")
                            .replace(
                                    account.charAt(account.length() - 1) + "', " + numberValues.get(1)
                                    , account.charAt(account.length() - 1) + "', '" + routing + "'");

                    System.out.printf("Substituted numbers: %s%n", valuesString);
                    String insertString = line.substring(0, line.lastIndexOf("("));
                    fixedInserts.add(insertString + valuesString);
                });
        Path dest = rootPath.resolve("dest_account_details.sql"); // will create or rewrite the file
        BufferedWriter writer = newBufferedWriter(dest);
        for (String line : fixedInserts) {
            System.out.println("writing line: \n" + line);
            writer.write(line + "\n");
        }
        writer.close();
    }


    /**
     * insert into address (address_id, country, city, street_name, street_number, postal_code) values (1, 'Benin', 'Tchaourou', 'Waxwing', '33', null);
     */
    @Test
    public void fixAddressDataTest() throws IOException {
        Path rootPath = get(TEST_FILES_DIR);
        Path source = rootPath.resolve("source_address.sql");

        List<String> newLines = new ArrayList<>();

        BufferedReader reader = newBufferedReader(source);
        reader.lines().forEach(str -> {
            // replace add_id with hash
            String s = str.replace("address_id", "address_hash");
            String insertString = s.substring(0, s.lastIndexOf('('));
            String valuesString = s.substring(s.lastIndexOf('('));
            // removing unnecessary chars
            StringBuilder valuesStringBuilder = new StringBuilder();
            valuesStringBuilder.append("(");
            valuesString = valuesString.replace("(", "");
            valuesString = valuesString.replace(")", "");
            valuesString = valuesString.replace(";", "");

            List<String> cleanValues = new ArrayList<>();
            String[] values = valuesString.split(", ");
            // skipping the first value for every line since it is a number.
            for (int i = 1; i < values.length; i++) {
                // skip comma for last value
                if (i + 1 == values.length) {
                    valuesStringBuilder.append(values[i]);
                } else {
                    valuesStringBuilder.append(values[i]).append(", ");
                }
                if (!values[i].equals("null")) { // some values for postal_code are "null" as strings.
                    cleanValues.add(values[i].replaceAll("'", ""));
                }
            }
            valuesStringBuilder.append(");");
            String normalizedAddressValuesString = normalize(cleanValues);
            String addressHash = "'" + generateAddressHash(normalizedAddressValuesString) + "'";
            valuesStringBuilder.insert(1, addressHash + ", "); // insert hash after open paren.
            newLines.add(insertString + valuesStringBuilder);
        });
        reader.close();

        Path dest = rootPath.resolve("dest_address.sql"); // will create or rewrite the file
        BufferedWriter writer = newBufferedWriter(dest);
        for (String line : newLines) {
            System.out.println("writing line: \n" + line);
            writer.write(line + "\n");
        }
        writer.close();
    }

    private String normalize(List<String> values) {
        StringBuilder normalized = new StringBuilder();
        values.forEach(value -> {
            String s = value.toUpperCase().trim().replaceAll("[^A-Z0-9 ]", "")
                    .replaceAll("\\bSTREET\\b", "ST")
                    .replaceAll("\\bAVENUE\\b", "AVE")
                    .replaceAll("\\bROAD\\b", "RD")
                    .replaceAll("\\bDRIVE\\b", "DR")
                    .replaceAll("\\s+", " ");
            normalized.append(s);
        });
        return normalized.toString();
    }

    /**
     * address hash for efficiently identifying a unique address.
     * 0xff & b:
     * 0xff (which is 11111111 in binary or 255 in decimal).
     * A byte is 8 bits and has a range from -128 to 127.
     * This means that when you deal with byte values, they are signed, and you might get negative values.
     * The & 0xff operation is used to convert the signed byte into an unsigned value.
     * The 0xff effectively masks the byte so that only the lower 8 bits (one byte) are considered, and any
     * sign bit is ignored. This ensures that the byte is treated as a value between 0 and 255 instead of -128 to 127.
     */
    private String generateAddressHash(String normalizedAddress) {
        try {
            MessageDigest digest = getInstance("SHA-256");

            // perform hashing
            byte[] hashBytes = digest.digest(normalizedAddress.getBytes());
            StringBuilder hexString = new StringBuilder();
            // convert byte array to hexadecimal string
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                //  ensure each byte is represented by exactly two characters in the final string.
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new AddressHashingFailedException(normalizedAddress, ex);
        }
    }

}
