<!--
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * IdentityManager.vue: User registration and MFA verification.
 * Features: Registers users, verifies with MFA.
-->
<template>
  <div class="card">
    <h2>Identity Management</h2>
    <div class="row">
      <div style="width:260px">
        <label class="small muted">User ID</label>
        <input v-model="userId" placeholder="Enter user ID" />
      </div>
      <div class="col">
        <label class="small muted">Public Key</label>
        <input v-model="publicKey" placeholder="Enter public key" />
      </div>
    </div>
    <div class="controls">
      <button @click="registerUser">Register User</button>
      <button @click="verifyUser">Verify User</button>
    </div>
    <div v-if="mfaToken" class="row">
      <div class="col">
        <label class="small muted">MFA Token</label>
        <input v-model="mfaInput" type="number" placeholder="Enter MFA token" />
      </div>
    </div>
    <p v-if="verificationResult">Verification: {{ verificationResult }}</p>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'IdentityManager',
  data() {
    return {
      userId: 'user123',
      publicKey: 'publicKeyExample',
      mfaToken: null,
      mfaInput: '',
      verificationResult: ''
    };
  },
  methods: {
    async registerUser() {
      try {
        const response = await axios.post('http://localhost:8080/api/register-user', {
          userId: this.userId,
          publicKey: this.publicKey
        });
        if (response.data.error) {
          alert(`Error: ${response.data.error}`);
        } else {
          this.mfaToken = response.data.mfa;
          alert(`Registered ${response.data.userId} — MFA: ${response.data.mfa}`);
          this.$emit('refresh-logs');
        }
      } catch (error) {
        alert(`Error: ${error.message}`);
      }
    },
    async verifyUser() {
      try {
        const response = await axios.post('http://localhost:8080/api/verify-user', {
          userId: this.userId,
          publicKey: this.publicKey,
          mfa: this.mfaInput
        });
        this.verificationResult = response.data.verified ? 'Success' : 'Failed';
        this.$emit('refresh-logs');
      } catch (error) {
        this.verificationResult = `Error: ${error.message}`;
      }
    }
  }
};
</script>

<style scoped>
.card {
  background: #15153d;
  padding: 14px;
  border-radius: 12px;
  margin-bottom: 12px;
}
.row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.controls {
  display: flex;
  gap: 8px;
}
button {
  background: linear-gradient(90deg, #7c5cff, #00d4ff);
  border: none;
  color: #021018;
  padding: 8px 12px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
input {
  background: #1b1b4d;
  border: 1px solid rgba(255,255,255,0.04);
  padding: 8px;
  border-radius: 8px;
  color: #fff;
}
.muted {
  color: #9aa6b2;
}
.small {
  font-size: 12px;
}
</style>
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */