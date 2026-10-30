(() => {
  const W = 1600, PAR = 0.3;
  const MS = window.__DECK_MS__ || ['Start', 'Wrap-up'];
  const stage = document.getElementById('stage'), track = document.getElementById('track'), bg = document.getElementById('bg');
  const tl = document.getElementById('tl'), notes = document.getElementById('notes'), ov = document.getElementById('overview');
  const hud = document.getElementById('hud');
  const slides = [...track.querySelectorAll('.slide')];
  const N = slides.length;
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const mc = m => `var(--m${m})`;
  const pad = n => String(n).padStart(2, '0');
  let cur = -1, raf = [];

  document.querySelectorAll('.nb').forEach(el => {
    const on = (el.dataset.on || '').split(',').filter(Boolean).map(Number);
    el.innerHTML = Array.from({ length: 9 }, (_, i) => `<i class="${on.includes(i) ? 'on' : ''}"></i>`).join('');
  });

  const fit = () => stage.style.setProperty('--scale', Math.min(innerWidth / W, innerHeight / 900));
  addEventListener('resize', fit); fit();

  bg.style.width = (W * 2 + Math.max(0, N - 1) * W * PAR) + 'px';
  slides.forEach((s, i) => {
    s.style.left = i * W + 'px';
    s.style.setProperty('--c', mc(s.dataset.m));
    const b = document.createElement('div');
    b.className = 'blob';
    b.style.left = (i * W * PAR + W / 2 - 700) + 'px';
    b.style.setProperty('--bc', mc(s.dataset.m));
    bg.appendChild(b);
  });

  const X0 = 150, X1 = 1450, step = N > 1 ? (X1 - X0) / (N - 1) : 0, x = i => X0 + i * step;
  const groups = [];
  slides.forEach((s, i) => {
    const m = +s.dataset.m, g = groups[groups.length - 1];
    if (g && g.m === m) g.to = i; else groups.push({ m, from: i, to: i });
  });
  let h = '<div class="tl-base"></div>';
  groups.forEach(g => {
    const l = x(g.from) - step / 2 + 5, r = x(g.to) + step / 2 - 5;
    h += `<div class="tl-seg" data-m="${g.m}" style="left:${l}px;width:${Math.max(0, r - l)}px;--mc:${mc(g.m)}"></div>`;
    const major = g.m >= 1 && g.m < MS.length - 1;
    h += `<button class="tl-lab${major ? '' : ' minor'}" data-m="${g.m}" data-go="${g.from}" style="left:${(x(g.from) + x(g.to)) / 2}px;--mc:${mc(g.m)}">${major ? `<b>${g.m}</b>` : ''}${MS[g.m] || ''}</button>`;
  });
  slides.forEach((s, i) => {
    const t = s.dataset.title.replace(/"/g, '&quot;');
    h += s.dataset.ai
      ? `<button class="tl-tick ai" data-go="${i}" style="left:${x(i)}px" title="${i + 1}. ${t}"><svg class="ic"><use href="#i-sparkles"/></svg></button>`
      : `<button class="tl-tick" data-go="${i}" style="left:${x(i)}px;--mc:${mc(s.dataset.m)}" title="${i + 1}. ${t}"></button>`;
  });
  h += '<div class="tl-mark"></div><button class="tl-nav" data-step="-1" style="left:70px" aria-label="Previous">‹</button><button class="tl-nav" data-step="1" style="left:1530px" aria-label="Next">›</button>';
  tl.innerHTML = h;
  const ticks = [...tl.querySelectorAll('.tl-tick')], mark = tl.querySelector('.tl-mark');
  tl.addEventListener('click', e => {
    const b = e.target.closest('[data-go],[data-step]');
    if (!b) return;
    b.dataset.go !== undefined ? go(+b.dataset.go) : go(cur + +b.dataset.step);
  });

  ov.querySelector('.ov-grid').innerHTML = slides.map((s, i) =>
    `<button class="ov-card" data-go="${i}" style="--mc:${mc(s.dataset.m)}"><span class="n">${pad(i + 1)} · ${MS[s.dataset.m] || ''}${s.dataset.ai ? ' · AI' : ''}</span><span class="t">${s.dataset.title}</span></button>`).join('');
  ov.addEventListener('click', e => { const b = e.target.closest('[data-go]'); if (b) { go(+b.dataset.go); ov.classList.remove('show'); } });

  const setFinal = s => s.querySelectorAll('[data-to]').forEach(el => el.textContent = (+el.dataset.to).toFixed(+(el.dataset.dec || 0)) + (el.dataset.suf || ''));
  const FIT_SEL = '.slide-safe .title,.s1-h,.s1-sub,.split-bullets .text,.gen-chips .chip,.journey-node .lbl,.s17-card b,.s6-row .rl,.doc-row .rl,.split-bullet .text';
  function clearFitSizes(slide) {
    slide.querySelectorAll(FIT_SEL).forEach(el => { el.style.fontSize = ''; });
  }
  function fitSlideCopy(slide) {
    if (reduced) return;
    const box = slide.querySelector('.slide-safe,.slide-hero');
    if (!box || slide.dataset.noFit !== undefined) return;
    clearFitSizes(slide);
    const targets = [...slide.querySelectorAll(FIT_SEL)];
    if (!targets.length) return;
    const floors = targets.map(el => parseFloat(getComputedStyle(el).fontSize) * 0.75);
    let guard = 0;
    while (guard++ < 24 && (box.scrollHeight > box.clientHeight + 1 || box.scrollWidth > box.clientWidth + 1)) {
      let shrunk = false;
      targets.forEach((el, i) => {
        const px = parseFloat(getComputedStyle(el).fontSize);
        if (px > floors[i]) {
          el.style.fontSize = (px * 0.94) + 'px';
          shrunk = true;
        }
      });
      if (!shrunk) break;
    }
  }
  function runCounters(s, baseMs) {
    raf.forEach(cancelAnimationFrame); raf = [];
    if (reduced) return setFinal(s);
    s.querySelectorAll('[data-to]').forEach(el => {
      const to = +el.dataset.to, from = +(el.dataset.from || 0), dec = +(el.dataset.dec || 0), suf = el.dataset.suf || '';
      const t0 = performance.now() + baseMs + +(el.dataset.delay || 0), du = +(el.dataset.dur || 1200);
      el.textContent = from.toFixed(dec) + suf;
      const tick = now => {
        const p = Math.min(1, Math.max(0, (now - t0) / du)), e = 1 - Math.pow(1 - p, 3);
        el.textContent = (from + (to - from) * e).toFixed(dec) + suf;
        if (p < 1) raf.push(requestAnimationFrame(tick));
      };
      raf.push(requestAnimationFrame(tick));
    });
  }

  function go(i, { replay = false } = {}) {
    i = Math.max(0, Math.min(N - 1, i));
    if (i === cur && !replay) return;
    const prev = cur; cur = i;
    const base = (prev < 0 || replay) ? 0.15 : 0.6;
    track.style.setProperty('--base', base + 's');
    if (prev < 0) { track.classList.add('instant'); bg.classList.add('instant'); }
    track.style.transform = `translateX(${-i * W}px)`;
    bg.style.transform = `translateX(${-i * W * PAR}px)`;
    if (prev >= 0 && prev !== i) { slides[prev].classList.remove('is-active'); clearFitSizes(slides[prev]); setFinal(slides[prev]); }
    const s = slides[i];
    s.classList.remove('is-active'); void s.offsetWidth; s.classList.add('is-active');
    if (prev < 0) requestAnimationFrame(() => requestAnimationFrame(() => { track.classList.remove('instant'); bg.classList.remove('instant'); }));
    runCounters(s, base * 1000);
    requestAnimationFrame(() => fitSlideCopy(s));

    const m = +s.dataset.m;
    hud.style.setProperty('--mc', mc(m));
    const majorHud = m >= 1 && m < MS.length - 1;
    hud.querySelector('.mtext').innerHTML = majorHud ? `Milestone ${m} · <b>${MS[m]}</b>` : `<b>${MS[m] || ''}</b>`;
    const ai = hud.querySelector('.ai');
    ai.classList.toggle('on', !!s.dataset.ai);
    ai.querySelector('span').textContent = s.dataset.ai ? `AI agent ${s.dataset.ai}` : '';
    hud.querySelector('.counter').innerHTML = `<b>${pad(i + 1)}</b> / ${pad(N)}`;
    ticks.forEach((t, k) => t.classList.toggle('past', k <= i));
    tl.querySelectorAll('.tl-seg,.tl-lab').forEach(el => el.classList.toggle('on', +el.dataset.m === m));
    mark.style.left = x(i) + 'px';
    mark.style.setProperty('--mc', s.dataset.ai ? 'var(--ai2)' : mc(m));
    const n = s.querySelector('.notes');
    notes.innerHTML = `<h4>${pad(i + 1)} · ${s.dataset.title}</h4>` + (n ? n.innerHTML : '');
    ov.querySelectorAll('.ov-card').forEach((c, k) => c.classList.toggle('cur', k === i));
    if (location.hash !== '#' + (i + 1)) history.replaceState(null, '', '#' + (i + 1));
  }

  const toggleFS = () => document.fullscreenElement ? document.exitFullscreen() : document.documentElement.requestFullscreen?.();
  addEventListener('keydown', e => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;
    const k = e.key;
    if (ov.classList.contains('show') && k !== 'o' && k !== 'O' && k !== 'Escape') return;
    if (['ArrowRight', 'PageDown', ' ', 'Enter', 'ArrowDown'].includes(k)) { e.preventDefault(); go(cur + 1); }
    else if (['ArrowLeft', 'PageUp', 'Backspace', 'ArrowUp'].includes(k)) { e.preventDefault(); go(cur - 1); }
    else if (k === 'Home') go(0);
    else if (k === 'End') go(N - 1);
    else if (k === 'f' || k === 'F') toggleFS();
    else if (k === 'n' || k === 'N') notes.classList.toggle('show');
    else if (k === 'r' || k === 'R') go(cur, { replay: true });
    else if (k === 'o' || k === 'O') ov.classList.toggle('show');
    else if (k === 'Escape') ov.classList.remove('show');
  });

  let sx = null, sy = null;
  const vp = document.getElementById('viewport');
  vp.addEventListener('pointerdown', e => { sx = e.clientX; sy = e.clientY; });
  vp.addEventListener('pointerup', e => {
    if (sx === null) return;
    const dx = e.clientX - sx, dy = e.clientY - sy;
    sx = null;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) go(cur + (dx < 0 ? 1 : -1));
  });

  const fromHash = () => { const n = parseInt(location.hash.slice(1), 10); return Number.isFinite(n) ? n - 1 : 0; };
  addEventListener('hashchange', () => go(fromHash()));
  go(fromHash());
})();
