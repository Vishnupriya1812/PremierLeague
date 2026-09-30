const API_BASE = '/api/premier-league';

// --- Tabs ---
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(btn.dataset.tab).classList.add('active');
  });
});

// --- Helpers ---
function showMessage(containerId, text, isError = false) {
  document.getElementById(containerId).innerHTML =
    `<p class="msg${isError ? ' error' : ''}">${escapeHtml(text)}</p>`;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message || body.error || detail;
    } catch (_) { /* body wasn't JSON */ }
    throw new Error(`${res.status} ${detail}`);
  }
  return res.json();
}

// --- Standings ---
async function loadStandings() {
  const seasonId = document.getElementById('standingsSeasonId').value;
  const containerId = 'standingsResult';
  showMessage(containerId, 'Loading…');
  try {
    const data = await fetchJson(`${API_BASE}/home/standings?seasonId=${seasonId}`);
    if (!data.length) return showMessage(containerId, 'No standings data yet — run the ingestion service first.');
    document.getElementById(containerId).innerHTML = `
      <table>
        <thead><tr><th>#</th><th>Team</th><th>P</th><th>W</th><th>D</th><th>L</th><th>GF</th><th>GA</th><th>GD</th><th>Pts</th></tr></thead>
        <tbody>
          ${data.map((t, i) => `
            <tr>
              <td>${i + 1}</td><td>${escapeHtml(t.name)}</td><td>${t.played}</td>
              <td>${t.won}</td><td>${t.drew}</td><td>${t.lost}</td>
              <td>${t.gf}</td><td>${t.ga}</td><td>${t.gd}</td><td><strong>${t.points}</strong></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  } catch (e) {
    showMessage(containerId, `Couldn't load standings: ${e.message}`, true);
  }
}

// --- Matches by round ---
async function loadMatches() {
  const round = document.getElementById('matchesRound').value;
  const containerId = 'matchesResult';
  showMessage(containerId, 'Loading…');
  try {
    const data = await fetchJson(`${API_BASE}/home/matchesByRound?round=${round}`);
    if (!data.length) return showMessage(containerId, `No matches found for round ${round}.`);
    document.getElementById(containerId).innerHTML = data.map(m => `
      <div class="card match-card">
        <div>
          <div class="match-teams">${escapeHtml(m.homeTeam)} vs ${escapeHtml(m.awayTeam)}</div>
          <div class="summary-meta">${m.venue ? escapeHtml(m.venue) + ' · ' : ''}${escapeHtml(m.status ?? '')}</div>
        </div>
        <div class="match-score">${m.homeScore ?? '-'} : ${m.awayScore ?? '-'}</div>
      </div>`).join('');
  } catch (e) {
    showMessage(containerId, `Couldn't load matches: ${e.message}`, true);
  }
}

// --- Team of the week ---
async function loadTopEleven() {
  const round = document.getElementById('topElevenRound').value;
  const containerId = 'topElevenResult';
  showMessage(containerId, 'Loading…');
  try {
    const data = await fetchJson(`${API_BASE}/home/topEleven?round=${round}`);
    if (!data.length) return showMessage(containerId, `No lineup data found for round ${round}.`);
    document.getElementById(containerId).innerHTML = `
      <table>
        <thead><tr><th>Player</th><th>Team</th><th>Position</th><th>Rating</th></tr></thead>
        <tbody>
          ${data.map(p => `
            <tr>
              <td>${escapeHtml(p.playerName)}</td><td>${escapeHtml(p.teamName)}</td>
              <td>${escapeHtml(p.position)}</td><td><strong>${p.rating?.toFixed(1) ?? '-'}</strong></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  } catch (e) {
    showMessage(containerId, `Couldn't load team of the week: ${e.message}`, true);
  }
}

// --- Top performers ---
async function loadTopPerformers() {
  const column = document.getElementById('statColumn').value;
  const label = document.getElementById('statColumn').selectedOptions[0].text;
  const containerId = 'topPerformersResult';
  showMessage(containerId, 'Loading…');
  try {
    const data = await fetchJson(`${API_BASE}/home/topPerformers?column=${column}`);
    if (!data.length) return showMessage(containerId, `No data for "${label}" yet.`);
    document.getElementById(containerId).innerHTML = `
      <table>
        <thead><tr><th>#</th><th>Player</th><th>Team</th><th>${escapeHtml(label)}</th></tr></thead>
        <tbody>
          ${data.map((p, i) => `
            <tr>
              <td>${i + 1}</td><td>${escapeHtml(p.playerName)}</td>
              <td>${escapeHtml(p.teamName)}</td><td><strong>${p.totalValue}</strong></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  } catch (e) {
    showMessage(containerId, `Couldn't load top performers: ${e.message}`, true);
  }
}

// --- AI match summary ---
async function loadAiSummary() {
  const matchId = document.getElementById('matchId').value;
  const containerId = 'aiSummaryResult';
  if (!matchId) return showMessage(containerId, 'Enter a match ID first.', true);
  showMessage(containerId, 'Asking Gemini for a recap… this can take a few seconds.');
  try {
    const data = await fetchJson(`${API_BASE}/matches/${matchId}/summary`);
    document.getElementById(containerId).innerHTML = `
      <div class="summary-meta">${escapeHtml(data.homeTeam ?? '')} ${data.homeScore ?? ''} - ${data.awayScore ?? ''} ${escapeHtml(data.awayTeam ?? '')}</div>
      <div class="summary-text">${escapeHtml(data.summary)}</div>`;
  } catch (e) {
    const friendly = e.message.includes('503')
      ? 'Gemini is temporarily at capacity (common on the free tier) — try again in a bit.'
      : e.message.includes('404')
      ? `No match found with ID ${matchId}.`
      : e.message;
    showMessage(containerId, friendly, true);
  }
}
