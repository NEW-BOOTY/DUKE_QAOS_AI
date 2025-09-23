<!--
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * QuantumUI.vue: Interface for quantum-classical task management.
 * Features: Triggers tasks via API, displays results.
-->
<template>
  <div class="card">
    <h2>Quantum Task Manager</h2>
    <div class="row">
      <div class="col">
        <label class="small muted">Task Name</label>
        <input v-model="taskName" placeholder="Enter task" />
      </div>
      <div style="width:160px">
        <label class="small muted">Mode</label>
        <select v-model="taskMode">
          <option>auto</option>
          <option>force-quantum</option>
          <option>force-classical</option>
        </select>
      </div>
    </div>
    <div class="controls">
      <button @click="runTask">Run Task</button>
    </div>
    <p>Task Result: {{ taskResult }}</p>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'QuantumUI',
  data() {
    return {
      taskName: 'AI Task',
      taskMode: 'auto',
      taskResult: '—'
    };
  },
  methods: {
    async runTask() {
      try {
        const response = await axios.post('http://localhost:8080/api/process-task', { task: this.taskName });
        this.taskResult = response.data.error ? `Error: ${response.data.error}` : `Result: ${response.data.result} — ${response.data.task}`;
        this.$emit('refresh-logs');
        this.$emit('refresh-performance');
      } catch (error) {
        this.taskResult = `Error: ${error.message}`;
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
input, select {
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