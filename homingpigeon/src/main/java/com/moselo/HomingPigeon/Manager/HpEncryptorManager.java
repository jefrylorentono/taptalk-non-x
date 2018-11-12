package com.moselo.HomingPigeon.Manager;

import com.moselo.HomingPigeon.Helper.AESCrypt;
import com.moselo.HomingPigeon.Const.HpDefaultConstant;
import com.moselo.HomingPigeon.Helper.HpUtils;

import java.security.GeneralSecurityException;

public class HpEncryptorManager {

    private static HpEncryptorManager instance;

    public static HpEncryptorManager getInstance() {
        return instance == null ? (instance = new HpEncryptorManager()) : instance;
    }

    public HpEncryptorManager() {

    }

    public String encrypt(String textToEncrypt, String id) throws GeneralSecurityException {
        String localKey, encrypted, encryptedWithSalt;
        char salt;
        int encryptedLength, saltIndex;
        int randomNumber = HpUtils.getInstance().generateRandomNumber(9);

        try {
            salt = id.charAt(textToEncrypt.length() % 32);
            localKey = new StringBuilder(HpUtils.getInstance().mySubString(id, 8, 16)).reverse().toString();
            encrypted = AESCrypt.encrypt(String.format("%s%s", HpDefaultConstant.ENCRYPTION_KEY, localKey), textToEncrypt);
            encryptedLength = encrypted.length();
            saltIndex = ((encryptedLength + randomNumber) * randomNumber) % encryptedLength;
            encryptedWithSalt = new StringBuilder(encrypted).insert(saltIndex, salt).insert(0, randomNumber).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
        return encryptedWithSalt;
    }

    public String decrypt(String textToDecrypt, String id) throws GeneralSecurityException {
        String localKey, encrypted, decrypted;
        int randomNumber, encryptedLength, saltIndex;
        try {
            localKey = new StringBuilder(HpUtils.getInstance().mySubString(id, 8, 16)).reverse().toString();
            randomNumber = Integer.parseInt(String.valueOf(textToDecrypt.charAt(0)));
            encryptedLength = textToDecrypt.length() - 2;
            saltIndex = ((encryptedLength + randomNumber) * randomNumber) % encryptedLength;
            encrypted = new StringBuilder(textToDecrypt).deleteCharAt(0).deleteCharAt(saltIndex).toString();
            decrypted = AESCrypt.decrypt(String.format("%s%s", HpDefaultConstant.ENCRYPTION_KEY, localKey), encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
        return decrypted;
    }
}
