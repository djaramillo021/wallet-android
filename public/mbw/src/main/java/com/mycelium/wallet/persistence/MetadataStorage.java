/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.persistence;

import android.content.Context;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MetadataStorage extends GenericMetadataStorage {
   public static final String ADDRESSLABEL_CATEGORY = "addresslabel";
   public static final String ACCOUNTLABEL_CATEGORY = "al";
   public static final String TRANSACTION_LABEL_CATEGORY = "tl";
   private static final KeyCategory SEED_BACKUPSTATE = new KeyCategory("seed", "backupstate");
   private static final KeyCategory PIN_RESET_BLOCKHEIGHT = new KeyCategory("pin", "reset_blockheight");
   private static final KeyCategory PIN_BLOCKHEIGHT = new KeyCategory("pin", "blockheight");

   public MetadataStorage(Context context) {
      super(context);
   }

   public void storeTransactionLabel(Sha256Hash txid, String label) {
      storeKeyCategoryValueEntry(txid.toString(), TRANSACTION_LABEL_CATEGORY, label);
   }

   public String getLabelByTransaction(Sha256Hash txid) {
      return getKeyCategoryValueEntry(txid.toString(), TRANSACTION_LABEL_CATEGORY, "");
   }

   public String getLabelByAccount(UUID account) {
      return getKeyCategoryValueEntry(account.toString(), ACCOUNTLABEL_CATEGORY, "");
   }

   public Optional<UUID> getAccountByLabel(String label) {
      Optional<String> account = getFirstKeyForCategoryValue(ACCOUNTLABEL_CATEGORY, label);

      if (account.isPresent()){
         return Optional.of(UUID.fromString(account.get()));
      }else{
         return Optional.absent();
      }
   }

   public void storeAccountLabel(UUID account, String label) {
      storeKeyCategoryValueEntry(account.toString(), ACCOUNTLABEL_CATEGORY, label);
   }

   public void deleteAccountMetadata(UUID account){
      deleteAllByKey(account.toString());
   }

   public Map<Address, String> getAllAddressLabels() {
      Map<String, String> entries = getKeysAndValuesByCategory(ADDRESSLABEL_CATEGORY);
      Map<Address, String> addresses = new HashMap<Address, String>();
      for (Map.Entry<String, String> e : entries.entrySet()) {
         String val = e.getValue();
         String key = e.getKey();
         addresses.put(Address.fromString(key), val);
      }
      return addresses;
   }

   public String getLabelByAddress(Address address) {
      return getKeyCategoryValueEntry(address.toString(), ADDRESSLABEL_CATEGORY, "");
   }

   public void deleteAddressMetadata(Address address) {
      // delete everything related to this address from metadata
      deleteAllByKey(address.toString());
   }

   public Optional<Address> getAddressByLabel(String label) {
      Optional<String> address = getFirstKeyForCategoryValue(ADDRESSLABEL_CATEGORY, label);

      if (address.isPresent()){
         return Optional.of(Address.fromString(address.get()));
      }else{
         return Optional.absent();
      }
   }

   public void storeAddressLabel(Address address, String label) {
      storeKeyCategoryValueEntry(address.toString(), ADDRESSLABEL_CATEGORY, label);
   }

   public void setIgnoreBackupWarning(UUID account, Boolean ignore){
      storeKeyCategoryValueEntry(account.toString(), "ibw", ignore ? "1" : "0");
   }

   public Boolean getIgnoreBackupWarning(UUID account){
      return  "1".equals(getKeyCategoryValueEntry(account.toString(), "ibw", "0"));
   }

   public boolean firstMasterseedBackupFinished(){
    return  getMasterSeedBackupState().equals(BackupState.VERIFIED);
   }

   public BackupState getMasterSeedBackupState() {
      return BackupState.fromString(
            getKeyCategoryValueEntry(SEED_BACKUPSTATE, BackupState.UNKNOWN.toString())
      );
   }

   public void deleteMasterKeyBackupAgeMs(){
      deleteByKeyCategory(SEED_BACKUPSTATE);
   }

   public Optional<Long> getMasterKeyBackupAgeMs(){
      Optional<String> lastBackup = getKeyCategoryValueEntry(SEED_BACKUPSTATE);
      if (lastBackup.isPresent()) {
         return Optional.of(Calendar.getInstance().getTimeInMillis() - Long.valueOf(lastBackup.get()));
      }else{
         return Optional.absent();
      }
   }

   public void setMasterKeyBackupState(BackupState state) {
      storeKeyCategoryValueEntry(SEED_BACKUPSTATE, state.toString());

      // if this is the first verified backup, remember the date
      if (state == BackupState.VERIFIED && getMasterSeedBackupState() != BackupState.VERIFIED){
         storeKeyCategoryValueEntry(SEED_BACKUPSTATE, String.valueOf(Calendar.getInstance().getTimeInMillis()) );
      }
   }

   public void setResetPinStartBlockheight(int blockChainHeight) {
      storeKeyCategoryValueEntry(PIN_RESET_BLOCKHEIGHT, String.valueOf(blockChainHeight));
   }

   public void clearResetPinStartBlockheight() {
      deleteByKeyCategory(PIN_RESET_BLOCKHEIGHT);
   }

   public Optional<Integer> getResetPinStartBlockHeight(){
      Optional<String> resetIn = getKeyCategoryValueEntry(PIN_RESET_BLOCKHEIGHT);
      if (resetIn.isPresent()){
         return Optional.of(Integer.valueOf(resetIn.get()));
      }else{
         return Optional.absent();
      }
   }

   public void setLastPinSetBlockheight(int blockChainHeight){
      storeKeyCategoryValueEntry(PIN_BLOCKHEIGHT, String.valueOf(blockChainHeight));
   }

   public void clearLastPinSetBlockheight(){
      deleteByKeyCategory(PIN_BLOCKHEIGHT);
   }

   public Optional<Integer> getLastPinSetBlockheight(){
      Optional<String> lastSet = getKeyCategoryValueEntry(PIN_BLOCKHEIGHT);
      if (lastSet.isPresent()){
         return Optional.of(Integer.valueOf(lastSet.get()));
      }else{
         return Optional.absent();
      }
   }

   public enum BackupState {
      UNKNOWN(0), VERIFIED(1), IGNORED(2);

      private final int _index;
      private BackupState(int index) {
         _index = index;
      }

      public static BackupState fromString(String state){
         return fromInt(Integer.parseInt(state));
      }

      public String toString(){
         return Integer.toString(_index);
      }

      public int toInt() {
         return _index;
      }
      public static BackupState fromInt(int integer) {
         switch (integer) {
            case 0:
               return BackupState.UNKNOWN;
            case 1:
               return BackupState.VERIFIED;
            case 2:
               return BackupState.IGNORED;
            default:
               return BackupState.UNKNOWN;
         }
      }
   }
}
