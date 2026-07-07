package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ISettingsStore;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Optional;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceStoreToSettingsStoreAdapter implements ISettingsStore {
   private final IPreferenceStore preferenceStore;
   private final IJson json;

   @Inject
   public PreferenceStoreToSettingsStoreAdapter(IPreferenceStore preferenceStore, IJson json) {
      Preconditions.checkNotNull(preferenceStore);
      Preconditions.checkNotNull(json);
      this.preferenceStore = preferenceStore;
      this.json = json;
   }

   public Optional<String> getString(String key) {
      try {
         return Optional.ofNullable(this.preferenceStore.getString(key));
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   public void setString(String key, String value) {
      try {
         this.preferenceStore.setValue(key, value);
      } catch (Exception var4) {
      }

   }

   public Optional<Integer> getInt(String key) {
      try {
         return Optional.ofNullable(this.preferenceStore.getInt(key));
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   public Optional<Boolean> getBoolean(String key) {
      try {
         return Optional.ofNullable(this.preferenceStore.getBoolean(key));
      } catch (Exception var3) {
         return Optional.empty();
      }
   }

   public <T> Optional<T> getValue(String key, Class<T> classOfT) {
      String value = this.preferenceStore.getString(key);
      return value == null ? Optional.empty() : this.json.deserialize(value, classOfT);
   }

   public <T> void setValue(String key, T value) {
      String serializedValue = this.json.serialize(value);
      this.preferenceStore.setValue(key, serializedValue);
   }
}
