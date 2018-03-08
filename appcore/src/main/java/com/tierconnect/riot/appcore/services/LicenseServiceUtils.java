package com.tierconnect.riot.appcore.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.io.FileUtils.*;

/**
 * Created by agutierrez on 5/6/15.
 */
public class LicenseServiceUtils {
    static Logger logger = Logger.getLogger(LicenseServiceUtils.class);

    public static Object fromBytes(byte[] data) throws Exception {
        try {
            ByteArrayInputStream b = new ByteArrayInputStream(data);
            ObjectInputStream o = new ObjectInputStream(b);
            return o.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] toBytes(Object object) throws Exception {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(object);
            return b.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PublicKey getDefaultPublicKey() {
        String pKey = "rO0ABXNyABRqYXZhLnNlY3VyaXR5LktleVJlcL35T7OImqVDAgAETAAJYWxnb3JpdGhtdAASTGphdmEvbGFuZy9TdHJpbmc7WwAHZW5jb2RlZHQAAltCTAAGZm9ybWF0cQB+AAFMAAR0eXBldAAbTGphdmEvc2VjdXJpdHkvS2V5UmVwJFR5cGU7eHB0AANSU0F1cgACW0Ks8xf4BghU4AIAAHhwAAABJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK7EMAXCH4AENUXgKWSs1AuGno+QwTflQerBF6hAXj5NkdQAv8jUktZi58dbJcY5L69VzAU13Aot5tzTNHZnnt0ty4nQFjJ+CenpOnd/yiCdLlVP5p1+fZ7x7dogYmH0pRPyqyxzKuH66fFYCvNjIXW8hz982heskoKAd0Svrgtzt+R3o0hwKcpqRlbS7ovCyQi0e9H8EpRvQ93xT3DR6oHTgA7b643h42bIkPVzIlPsmcHC6MpNq26hKhsdPNdhP/hdgPZLeTTwUUPcXXJMCA462aGqaurpdQMIvjchfCwLWH4DDihFQIs1loRkOoP5lxq8XaidMhPUjloiHzm/irMCAwEAAXQABVguNTA5fnIAGWphdmEuc2VjdXJpdHkuS2V5UmVwJFR5cGUAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAZQVUJMSUM=";
        PublicKey publicKey = null;
        try {
            publicKey = (PublicKey) fromBytes(org.apache.commons.codec.binary.Base64.decodeBase64(pKey));
        } catch (Exception e) {
            return null;
        }
        return publicKey;
    }

    public static String getPublicKeyAsB64String(PublicKey publicKey) {
        try {
            return new String(org.apache.commons.codec.binary.Base64.encodeBase64(toBytes(publicKey)), Charsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean verifySignature(TreeMap<String, Object> licenseAsMap, PublicKey publicKey) {
        try {

            String jwsCompactSerialization = (String) licenseAsMap.get("key");
            // Create a new JsonWebSignature
            JsonWebSignature jws = new JsonWebSignature();

            // Set the compact serialization on the JWS
            jws.setCompactSerialization(jwsCompactSerialization);

            // Set the verification key
            // Note that your application will need to determine where/how to get the key
            // Here we use an example from the JWS spec
            jws.setKey(publicKey);

            // Check the signature
            boolean signatureVerified = jws.verifySignature();

            if (!signatureVerified) {
                return false;
            }

            // Do something useful with the result of signature verification
            logger.debug("JWS Signature is valid: " + signatureVerified);

            // Get the payload, or signed content, from the JWS
            String payload = jws.getPayload();

            // Do something useful with the content
            logger.debug("JWS payload: " + payload);

            TreeMap<String, Object> licenseAsMapInternal = licenseStringToMap(payload);

            for (Map.Entry<String, Object> entry : licenseAsMapInternal.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                Object value2 = licenseAsMap.get(key);
                if (!key.equals("key") && !key.equals("group") && !key.equals("groupId")) {
                    if (value instanceof Collection && value2 instanceof Collection) {
                        boolean equals = new ArrayList((Collection) value).equals(new ArrayList((Collection) value2));
                        if (!equals) {
                            logger.error(" license file invalid, property: " + key + " expected: " + value + " real: " + value2);
                            return false;
                        }
                    } else {
                        boolean equals = Objects.equals(value, value2);
                        if (!equals) {
                            logger.error(" license file invalid, property: " + key + " expected: " + value + " real: " + value2);
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    public static String generateSignature(TreeMap<String, Object> licenseAsMap, PrivateKey privateKey) throws Exception {

        // The content that will be signed
        String examplePayload = licenseMapToString(licenseAsMap);

        // Create a new JsonWebSignature
        JsonWebSignature jws = new JsonWebSignature();

        // Set the payload, or signed content, on the JWS object
        jws.setPayload(examplePayload);

        // Set the signature algorithm on the JWS that will integrity protect the payload
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA512);

        // Set the signing key on the JWS
        // Note that your application will need to determine where/how to get the key
        // and here we just use an example from the JWS spec
        jws.setKey(privateKey);

        // Sign the JWS and produce the compact serialization or complete JWS representation, which
        // is a string consisting of three dot ('.') separated base64url-encoded
        // parts in the form Header.Payload.Signature
        String jwsCompactSerialization = null;
        try {
            jwsCompactSerialization = jws.getCompactSerialization();
            // Do something useful with your JWS
            return jwsCompactSerialization;
        } catch (JoseException e) {
            return null;
        }
    }

    /*
    public static String calculateHash(TreeMap<String, Object> licenseAsMap) {
        StringBuilder stringToHash = new StringBuilder();
        for (Map.Entry<String, Object> entry : licenseAsMap.entrySet()) {
            String property = entry.getKey();
            Object value = entry.getValue();
            if (!"groupId".equals(property) && !"key".equals(property)) {
                stringToHash.append(property + ":" + (value != null ? value.toString() : ""));
            }
        }
        return HashUtils.hashSHA256(stringToHash.toString());
    }*/

    public static TreeMap<String, Object> licenseStringToMap(String licenseString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            //Canonicalization
            TreeMap<String, Object> licenseAsMap = (TreeMap<String, Object>) objectMapper.readValue(licenseString, TreeMap.class);
            for (Map.Entry<String, Object> entry : licenseAsMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Collection) {
                    Collections.sort((List) value);
                }
            }
            return licenseAsMap;
        } catch (IOException e) {
            return null;
        }
    }

    public static LicenseDetail licenseStringToLicenseDetail(String licenseString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(licenseString, LicenseDetail.class);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static String licenseMapToString(TreeMap<String, Object> licenseAsMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(licenseAsMap);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static TreeMap<String, Object> licenseDetailToMap(LicenseDetail licenseDetail) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(licenseDetail);
            return licenseStringToMap(jsonString);
        } catch (Exception e) {
            return null;
        }
    }

    public static void generateCustomRiotKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        writeByteArrayToFile(new File("riot.kp"), toBytes(keyPair));
        KeyPair keyPair2 = ((KeyPair) fromBytes(readFileToByteArray(new File("riot.kp"))));
        if (Arrays.hashCode(keyPair.getPrivate().getEncoded()) == Arrays.hashCode(keyPair2.getPrivate().getEncoded())) {
            System.out.println("llave correcta");
        }
    }

    //readFileToByteArray(byteArray)
    public static KeyPair loadPem(byte[] byteArray, String password)  {
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(byteArray), StandardCharsets.UTF_8));
        KeyPair keyPair = null;
        try {
            Object o = pemParser.readObject();
            if (o != null) {
                if (o instanceof PEMEncryptedKeyPair) {
                    //Logger.info("Encrypted key - we will use provided password");
                    PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                    keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(decProv));
                } else if (o instanceof PEMKeyPair) {
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                    //Logger.info("Unencrypted key - no password needed");
                    keyPair = converter.getKeyPair((PEMKeyPair) o);
                }
            }
        } catch (IOException e) {
            logger.error("PEM File Error!", e);
        }
        return keyPair;
    }

    public static void main(String[] args) throws Exception {
        KeyPair keyPair = loadPem(readFileToByteArray(new File("../riot.pem")), "");
        //KeyPair keyPair = loadPem(readFileToByteArray(new File("../riot_wp.pem")), "xxxx");
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("If you want to use this license use this publicKey on Base64: " + LicenseServiceUtils.getPublicKeyAsB64String(keyPair.getPublic()));
        logger.warn("If you want to use this license use this publicKey on Base64: " + LicenseServiceUtils.getPublicKeyAsB64String(keyPair.getPublic()));

        LicenseDetail licenseDetail = new LicenseDetail();
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(Calendar.YEAR, 2016);
        gc.set(Calendar.MONTH, 4);
        gc.set(Calendar.DAY_OF_MONTH, 25);
        gc.set(Calendar.HOUR_OF_DAY, 23);
        gc.set(Calendar.MINUTE, 59);
        Date time = gc.getTime();
        System.out.println("Time" + time);
        licenseDetail.setCreationDate(new Date());
        licenseDetail.setExpirationDate(time);
        licenseDetail.setModules(Arrays.asList("Things", "Tenants", "Edge", "Gateway", "Flows", "Model", "Services", "Analytics"));
        licenseDetail.setFeatures(Arrays.asList(LicenseDetail.INTEGRATION_SAP_FMC, LicenseDetail.INTEGRATION_GENETEC_BAKU, LicenseDetail.INTEGRATION_MD_7_BAKU, LicenseDetail.SERVICES_LICENSE_GENERATOR));
        licenseDetail.setApplicableGroupLevel(Arrays.asList(1L, 2L));
        licenseDetail.setClientIp("ANY");
        licenseDetail.setCustomer("Coderoad");
        licenseDetail.setDescription("Coderoad Devs, QA and DevOps");
        licenseDetail.setLicenseType("DEV");
        licenseDetail.setMaxConcurrentUsers(null);
        licenseDetail.setMaxLevel2Groups(null);
        licenseDetail.setMaxLevel3Groups(null);
        licenseDetail.setMaxNumberOfUsers(null);
        licenseDetail.setMaxThings(null);
        licenseDetail.setMaxThingTypes(null);
        licenseDetail.setServerIp("ANY");
        licenseDetail.setProduct("RIoT");
        licenseDetail.setVendor("Mojix");
        licenseDetail.setVersion("4.2.x");
        System.out.println("License:\n\n" + generateLicense(licenseDetail, privateKey));

    }

    private static String generateLicense(LicenseDetail licenseDetail, PrivateKey privateKey) throws Exception {
        TreeMap<String, Object> licenseAsMap = LicenseServiceUtils.licenseDetailToMap(licenseDetail);
        if (licenseAsMap == null) {
            return null;
        }
        licenseAsMap.put("serialNumber", new SimpleDateFormat("YYYYMMddhhmmss").format(new Date()));
        String signature = LicenseServiceUtils.generateSignature(licenseAsMap, privateKey);
        licenseAsMap.put("key", signature);
        return LicenseServiceUtils.licenseMapToString(licenseAsMap);
    }
}


