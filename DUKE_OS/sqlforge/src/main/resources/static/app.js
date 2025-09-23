// Minimal SPA JS to interact with API
// Copyright © 2025 Devin B. Royal. All Rights Reserved.
(() => {
  const nlEl = document.getElementById('nl');
  const runBtn = document.getElementById('run');
  const explainBtn = document.getElementById('explain');
  const nl2sqlBtn = document.getElementById('nl2sql');
  const modeSel = document.getElementById('mode');
  const resultEl = document.getElementById('result');
  const adviceEl = document.getElementById('advice');
  const historyEl = document.getElementById('history');

  async function postJson(path, obj) {
    const res = await fetch(path, {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(obj)
    });
    return res.json();
  }

  async function run(mode) {
    const text = nlEl.value.trim();
    if (!text) return alert('Enter SQL or NL text');
    resultEl.textContent = 'Running...';
    try {
      const resp = await postJson('/api/run', { sql: text, mode: mode, userId: 'local' });
      if (resp.ok) {
        resultEl.textContent = JSON.stringify({ rowCount: resp.rowCount, rows: resp.rows }, null, 2);
      } else {
        resultEl.textContent = 'Error: ' + resp.message;
      }
      await loadAdvice(text);
      await loadHistory();
    } catch (e) {
      resultEl.textContent = 'Network error: ' + e.message;
    }
  }

  async function explain() {
    const text = nlEl.value.trim();
    if (!text) return alert('Enter SQL for explain');
    resultEl.textContent = 'Explaining...';
    try {
      const resp = await postJson('/api/explain', { sql: text });
      if (resp.ok) {
        resultEl.textContent = JSON.stringify(resp.rows || resp.message || resp, null, 2);
      } else {
        resultEl.textContent = 'Error: ' + resp.message;
      }
    } catch (e) {
      resultEl.textContent = 'Network error: ' + e.message;
    }
  }

  async function nl2sql() {
    const text = nlEl.value.trim();
    if (!text) return alert('Enter NL text');
    resultEl.textContent = 'Converting...';
    try {
      const resp = await postJson('/api/nl-to-sql', { sql: text });
      if (resp.ok) {
        nlEl.value = resp.sql || resp.message || '';
        resultEl.textContent = 'Converted to SQL.';
      } else {
        resultEl.textContent = 'Error: ' + resp.message;
      }
    } catch (e) {
      resultEl.textContent = 'Network error: ' + e.message;
    }
  }

  async function loadAdvice(sql) {
    try {
      const resp = await postJson('/api/advice', { sql: sql });
      if (resp.ok) {
        adviceEl.textContent = JSON.stringify(resp, null, 2);
      } else {
        adviceEl.textContent = 'No advice: ' + resp.message;
      }
    } catch (e) {
      adviceEl.textContent = 'Advice error: ' + e.message;
    }
  }

  async function loadHistory() {
    try {
      const res = await fetch('/api/history?userId=local');
      const arr = await res.json();
      historyEl.innerHTML = '';
      for (const q of arr) {
        const li = document.createElement('li');
        li.textContent = q;
        li.onclick = () => { nlEl.value = q; };
        historyEl.appendChild(li);
      }
    } catch (e) {
      console.warn('History load failed', e);
    }
  }

  runBtn.onclick = () => run(modeSel.value);
  explainBtn.onclick = explain;
  nl2sqlBtn.onclick = nl2sql;

  // initial load
  loadHistory();
})();
