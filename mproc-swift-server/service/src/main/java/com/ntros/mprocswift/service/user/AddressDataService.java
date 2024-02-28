package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.dto.AddressDTO;
import com.ntros.mprocswift.exceptions.AddressConstraintFailureException;
import com.ntros.mprocswift.exceptions.AddressHashingFailedException;
import com.ntros.mprocswift.exceptions.AddressNotFoundException;
import com.ntros.mprocswift.model.Address;
import com.ntros.mprocswift.repository.user.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AddressDataService implements AddressService {

    protected static final Logger log = LoggerFactory.getLogger(AddressDataService.class);

    private final AddressRepository addressRepository;

    @Autowired
    public AddressDataService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public Address getAddress(int addressId) {
        return addressRepository
                .findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    @Override
    public Address createAddress(Address address) {
        try {
            return addressRepository.save(address);
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to persist address with data:{}\n error is: {}", address, ex.getMessage());
            throw new AddressConstraintFailureException(ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public Address createAddress(AddressDTO addressDTO) {
        Address address = new Address();
        String normalizedAddressString = normalizeAddress(addressDTO, address);
        String addressHash = generateAddressHash(normalizedAddressString);
        address.setAddressHash(addressHash);
        return createAddress(address);
    }


    private String normalizeAddress(AddressDTO addressDTO, Address address) {
        // convert to upper case
        String country = addressDTO.getCountry().toUpperCase().trim();
        String city = addressDTO.getCity().toUpperCase().trim();
        String streetName = addressDTO.getStreetName().toUpperCase().trim();
        String streetNumber = addressDTO.getStreetNumber().toUpperCase().trim();
        String postalCode = addressDTO.getPostalCode().toUpperCase().trim();

        // remove non-alphanumeric characters except spaces
        country = country.replaceAll("[^A-Z0-9 ]", "");
        city = city.replaceAll("[^A-Z0-9 ]", "");
        streetName = streetName.replaceAll("[^A-Z0-9 ]", "");
        streetNumber = streetNumber.replaceAll("[^A-Z0-9 ]", "");
        postalCode = postalCode.replaceAll("[^A-Z0-9 ]", "");

        // replace common street suffixes
        streetName = streetName.replaceAll("\\bSTREET\\b", "ST")
                .replaceAll("\\bAVENUE\\b", "AVE")
                .replaceAll("\\bROAD\\b", "RD")
                .replaceAll("\\bDRIVE\\b", "DR");

        // trim spaces
        country = country.replaceAll("\\s+", " ");
        city = city.replaceAll("\\s+", " ");
        streetName = streetName.replaceAll("\\s+", " ");
        streetNumber = streetNumber.replaceAll("\\s+", " ");
        postalCode = postalCode.replaceAll("\\s+", " ");

        address.setCountry(country);
        address.setCity(city);
        address.setStreetName(streetName);
        address.setStreetNumber(streetNumber);
        address.setPostalCode(postalCode);

        return country + city + streetName + streetNumber + postalCode;
    }

    /**
     * insert into address (address_id, country, city, street_name, street_number, postal_code) values (1, 'Benin', 'Tchaourou', 'Waxwing', '33', null);
     */


    @Override
    public void generateHashedAddressInsertStatements(String sourceFile, String destFile) throws URISyntaxException, IOException {
        Path source = Paths.get(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("sql/data/address.sql")).toURI());
//        Path dest = Paths.get(Objects.requireNonNull(getClass().getClassLoader()
//                .getResource("sql/01_address.sql")).toURI());

        List<String> newLines = new ArrayList<>();

        BufferedReader reader = Files.newBufferedReader(source);
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
                if (!values[i].equals("null")) { // some vals for postal_code are "null" as strings.
                    cleanValues.add(values[i].replaceAll("'", ""));
                }
            }
            valuesStringBuilder.append(");");
            String normalizedAddressValuesString = normalize(cleanValues);
            String addressHash = "'" + generateAddressHash(normalizedAddressValuesString) + "'";
            valuesStringBuilder.insert(1, addressHash + ", "); // insert hash after open paren.
            System.out.println("Values string builder: " + valuesStringBuilder);
            String combined = insertString + valuesStringBuilder;
            newLines.add(combined);
        });
        reader.close();

        Path dest = Paths.get("scripts/mysql").resolve("01_address.sql");
        BufferedWriter writer = Files.newBufferedWriter(dest);
        for(String line: newLines) {
            System.out.println("writing line: \n" + line);
            writer.write(line + "\n");
        }
        writer.close();
    }

    private String normalize(List<String> values) {
        StringBuilder normalizedString = new StringBuilder();
        values.forEach(value -> {
            String s = value.toUpperCase().trim().replaceAll("[^A-Z0-9 ]", "")
                    .replaceAll("\\bSTREET\\b", "ST")
                    .replaceAll("\\bAVENUE\\b", "AVE")
                    .replaceAll("\\bROAD\\b", "RD")
                    .replaceAll("\\bDRIVE\\b", "DR")
                    .replaceAll("\\s+", " ");
            normalizedString.append(s);
        });
        return normalizedString.toString();
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
     *
     * @param normalizedAddress
     * @return
     */
    private String generateAddressHash(String normalizedAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

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
