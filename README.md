<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Nexus One — README</title>
<link href="https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;600&display=swap" rel="stylesheet">
<style>
  :root {
    --purple: #5B21B6;
    --violet: #7C3AED;
    --indigo: #4F46E5;
    --pink: #EC4899;
    --cyan: #06B6D4;
    --gold: #F59E0B;
    --green: #10B981;
    --red: #EF4444;
    --bg: #07060F;
    --surface: #0F0D1A;
    --card: #15122A;
    --border: rgba(124,58,237,0.25);
    --text: #E2E0FF;
    --muted: #8B7FBB;
  }

  * { margin: 0; padding: 0; box-sizing: border-box; }

  body {
    background: var(--bg);
    color: var(--text);
    font-family: 'Syne', sans-serif;
    overflow-x: hidden;
    min-height: 100vh;
  }

  /* Animated background grid */
  body::before {
    content: '';
    position: fixed;
    inset: 0;
    background-image:
      linear-gradient(rgba(124,58,237,0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(124,58,237,0.04) 1px, transparent 1px);
    background-size: 40px 40px;
    animation: gridMove 20s linear infinite;
    pointer-events: none;
    z-index: 0;
  }

  @keyframes gridMove {
    0% { transform: translateY(0); }
    100% { transform: translateY(40px); }
  }

  /* Floating orbs */
  .orb {
    position: fixed;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.15;
    animation: orbFloat 12s ease-in-out infinite alternate;
    pointer-events: none;
    z-index: 0;
  }
  .orb-1 { width: 500px; height: 500px; background: var(--violet); top: -100px; left: -100px; animation-delay: 0s; }
  .orb-2 { width: 400px; height: 400px; background: var(--cyan); bottom: -50px; right: -80px; animation-delay: -6s; }
  .orb-3 { width: 300px; height: 300px; background: var(--pink); top: 50%; left: 50%; animation-delay: -3s; }

  @keyframes orbFloat {
    0% { transform: translate(0, 0) scale(1); }
    100% { transform: translate(30px, -30px) scale(1.1); }
  }

  .container {
    position: relative;
    z-index: 1;
    max-width: 960px;
    margin: 0 auto;
    padding: 40px 24px 80px;
  }

  /* ===== HERO ===== */
  .hero {
    text-align: center;
    padding: 80px 0 60px;
    animation: fadeUp 0.8s ease both;
  }

  @keyframes fadeUp {
    from { opacity: 0; transform: translateY(30px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .logo-ring {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--violet), var(--indigo));
    box-shadow: 0 0 60px rgba(124,58,237,0.5), 0 0 120px rgba(124,58,237,0.2);
    font-size: 48px;
    margin-bottom: 32px;
    animation: pulseGlow 3s ease-in-out infinite;
  }

  @keyframes pulseGlow {
    0%, 100% { box-shadow: 0 0 60px rgba(124,58,237,0.5), 0 0 120px rgba(124,58,237,0.2); }
    50% { box-shadow: 0 0 80px rgba(124,58,237,0.8), 0 0 160px rgba(124,58,237,0.4); }
  }

  .hero h1 {
    font-size: clamp(3rem, 8vw, 5.5rem);
    font-weight: 800;
    letter-spacing: -2px;
    line-height: 1;
    background: linear-gradient(135deg, #fff 0%, var(--violet) 40%, var(--cyan) 80%, #fff 100%);
    background-size: 300% 300%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: gradientShift 5s ease-in-out infinite;
    margin-bottom: 8px;
  }

  @keyframes gradientShift {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
  }

  .hero-subtitle {
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.85rem;
    color: var(--muted);
    letter-spacing: 4px;
    text-transform: uppercase;
    margin-bottom: 24px;
  }

  .hero-desc {
    max-width: 620px;
    margin: 0 auto 36px;
    font-size: 1.1rem;
    color: #B8B0DD;
    line-height: 1.7;
    font-weight: 400;
  }

  /* Badge row */
  .badges {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;
    margin-bottom: 20px;
  }

  .badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: 100px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.72rem;
    font-weight: 600;
    letter-spacing: 0.5px;
    border: 1px solid;
    transition: transform 0.2s, box-shadow 0.2s;
    cursor: default;
  }
  .badge:hover { transform: translateY(-2px); }

  .badge-java { background: rgba(239,68,68,0.12); border-color: rgba(239,68,68,0.4); color: #FCA5A5; }
  .badge-spring { background: rgba(16,185,129,0.12); border-color: rgba(16,185,129,0.4); color: #6EE7B7; }
  .badge-mysql { background: rgba(245,158,11,0.12); border-color: rgba(245,158,11,0.4); color: #FCD34D; }
  .badge-thyme { background: rgba(245,158,11,0.12); border-color: rgba(245,158,11,0.4); color: #FCD34D; }
  .badge-kyau { background: rgba(124,58,237,0.15); border-color: rgba(124,58,237,0.5); color: #C4B5FD; }
  .badge-2026 { background: rgba(236,72,153,0.12); border-color: rgba(236,72,153,0.4); color: #F9A8D4; }

  /* ===== DIVIDER ===== */
  .divider {
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--violet), var(--cyan), transparent);
    margin: 50px 0;
    animation: shimmer 3s linear infinite;
    background-size: 200% 100%;
  }
  @keyframes shimmer {
    0% { background-position: -200% 0; }
    100% { background-position: 200% 0; }
  }

  /* ===== SECTION TITLE ===== */
  .section-title {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 32px;
    animation: fadeUp 0.6s ease both;
  }

  .section-icon {
    font-size: 1.5rem;
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: linear-gradient(135deg, var(--violet), var(--indigo));
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 4px 20px rgba(124,58,237,0.35);
    flex-shrink: 0;
  }

  .section-title h2 {
    font-size: 1.6rem;
    font-weight: 800;
    letter-spacing: -0.5px;
    background: linear-gradient(90deg, #fff, #C4B5FD);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  /* ===== TECH STACK ===== */
  .tech-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 16px;
    margin-bottom: 20px;
  }

  .tech-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 20px 18px;
    display: flex;
    align-items: center;
    gap: 14px;
    transition: transform 0.3s, border-color 0.3s, box-shadow 0.3s;
    animation: fadeUp 0.6s ease both;
  }

  .tech-card:hover {
    transform: translateY(-4px);
    border-color: rgba(124,58,237,0.6);
    box-shadow: 0 12px 40px rgba(124,58,237,0.2);
  }

  .tech-icon { font-size: 1.8rem; flex-shrink: 0; }
  .tech-name { font-size: 0.9rem; font-weight: 700; color: #E2E0FF; }
  .tech-role { font-size: 0.72rem; color: var(--muted); font-family: 'JetBrains Mono', monospace; margin-top: 2px; }

  /* ===== FEATURES GRID ===== */
  .feature-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .feature-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 28px 24px;
    position: relative;
    overflow: hidden;
    transition: transform 0.3s, box-shadow 0.3s;
    animation: fadeUp 0.7s ease both;
  }

  .feature-card::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: var(--accent, linear-gradient(90deg, var(--violet), var(--cyan)));
  }

  .feature-card:hover {
    transform: translateY(-6px);
    box-shadow: 0 20px 60px rgba(0,0,0,0.4);
  }

  .feature-emoji {
    font-size: 2rem;
    margin-bottom: 14px;
    display: block;
    animation: bounce 2s ease-in-out infinite;
    animation-play-state: paused;
  }
  .feature-card:hover .feature-emoji { animation-play-state: running; }
  @keyframes bounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-6px); }
  }

  .feature-title { font-size: 1rem; font-weight: 700; margin-bottom: 8px; color: #fff; }
  .feature-list { list-style: none; padding: 0; }
  .feature-list li {
    font-size: 0.82rem;
    color: var(--muted);
    padding: 4px 0;
    padding-left: 16px;
    position: relative;
    font-family: 'JetBrains Mono', monospace;
    line-height: 1.5;
  }
  .feature-list li::before {
    content: '›';
    position: absolute;
    left: 0;
    color: var(--violet);
  }

  /* ===== UML DIAGRAM ===== */
  .uml-container {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 32px;
    overflow-x: auto;
  }

  .uml-title {
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.75rem;
    color: var(--cyan);
    letter-spacing: 2px;
    text-transform: uppercase;
    margin-bottom: 24px;
    display: flex; align-items: center; gap: 8px;
  }
  .uml-title::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, var(--border), transparent);
  }

  .uml-svg-wrap {
    display: flex;
    justify-content: center;
  }

  /* ===== ROLES TABLE ===== */
  .roles-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
  }
  .roles-table th {
    background: rgba(124,58,237,0.2);
    color: #C4B5FD;
    padding: 12px 18px;
    text-align: left;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.72rem;
    letter-spacing: 1px;
    text-transform: uppercase;
    border-bottom: 1px solid var(--border);
  }
  .roles-table td {
    padding: 12px 18px;
    border-bottom: 1px solid rgba(124,58,237,0.1);
    color: #C4B5FD;
    vertical-align: middle;
  }
  .roles-table tr:last-child td { border-bottom: none; }
  .roles-table tr:hover td { background: rgba(124,58,237,0.06); }

  .role-tag {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 100px;
    font-size: 0.7rem;
    font-weight: 700;
    font-family: 'JetBrains Mono', monospace;
  }
  .tag-admin { background: rgba(239,68,68,0.15); color: #FCA5A5; border: 1px solid rgba(239,68,68,0.3); }
  .tag-teacher { background: rgba(16,185,129,0.15); color: #6EE7B7; border: 1px solid rgba(16,185,129,0.3); }
  .tag-student { background: rgba(124,58,237,0.2); color: #C4B5FD; border: 1px solid rgba(124,58,237,0.4); }

  /* ===== SETUP STEPS ===== */
  .steps { display: flex; flex-direction: column; gap: 16px; }

  .step {
    display: flex;
    gap: 20px;
    align-items: flex-start;
    animation: fadeUp 0.6s ease both;
  }

  .step-num {
    width: 36px; height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--violet), var(--indigo));
    display: flex; align-items: center; justify-content: center;
    font-weight: 800;
    font-size: 0.9rem;
    flex-shrink: 0;
    box-shadow: 0 4px 16px rgba(124,58,237,0.4);
  }

  .step-content { flex: 1; }
  .step-content h4 { font-size: 0.95rem; font-weight: 700; margin-bottom: 6px; }
  .step-content code {
    display: block;
    background: rgba(0,0,0,0.4);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 10px 16px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.78rem;
    color: var(--cyan);
    overflow-x: auto;
    white-space: pre;
  }

  /* ===== SCREENS GALLERY ===== */
  .screens-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }

  .screen-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 16px;
    overflow: hidden;
    transition: transform 0.3s, box-shadow 0.3s;
    animation: fadeUp 0.6s ease both;
  }
  .screen-card:hover {
    transform: translateY(-6px) scale(1.02);
    box-shadow: 0 20px 60px rgba(124,58,237,0.3);
    border-color: rgba(124,58,237,0.5);
  }

  .screen-thumb {
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2rem;
    position: relative;
    overflow: hidden;
  }

  .screen-thumb::before {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0.15;
  }

  .screen-label {
    padding: 10px 14px;
    font-size: 0.8rem;
    font-weight: 700;
    color: #C4B5FD;
    font-family: 'JetBrains Mono', monospace;
    border-top: 1px solid var(--border);
  }

  /* ===== FOOTER ===== */
  .footer {
    text-align: center;
    padding: 60px 0 20px;
    animation: fadeUp 0.8s ease both;
  }

  .footer-logo {
    font-size: 2rem;
    font-weight: 800;
    background: linear-gradient(135deg, var(--violet), var(--cyan));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 12px;
  }

  .footer p {
    color: var(--muted);
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.75rem;
    letter-spacing: 1px;
  }

  .footer-links {
    display: flex;
    gap: 20px;
    justify-content: center;
    margin: 20px 0;
    flex-wrap: wrap;
  }

  .footer-link {
    color: var(--violet);
    font-size: 0.82rem;
    font-weight: 600;
    text-decoration: none;
    padding: 6px 16px;
    border: 1px solid var(--border);
    border-radius: 100px;
    transition: all 0.2s;
    font-family: 'JetBrains Mono', monospace;
  }
  .footer-link:hover {
    background: rgba(124,58,237,0.15);
    border-color: var(--violet);
    color: #C4B5FD;
  }

  /* Stagger animations */
  .tech-card:nth-child(1) { animation-delay: 0.05s; }
  .tech-card:nth-child(2) { animation-delay: 0.1s; }
  .tech-card:nth-child(3) { animation-delay: 0.15s; }
  .tech-card:nth-child(4) { animation-delay: 0.2s; }
  .tech-card:nth-child(5) { animation-delay: 0.25s; }
  .tech-card:nth-child(6) { animation-delay: 0.3s; }
  .feature-card:nth-child(1) { animation-delay: 0.05s; }
  .feature-card:nth-child(2) { animation-delay: 0.1s; }
  .feature-card:nth-child(3) { animation-delay: 0.15s; }
  .feature-card:nth-child(4) { animation-delay: 0.2s; }
  .feature-card:nth-child(5) { animation-delay: 0.25s; }
  .feature-card:nth-child(6) { animation-delay: 0.3s; }
  .feature-card:nth-child(7) { animation-delay: 0.35s; }
  .feature-card:nth-child(8) { animation-delay: 0.4s; }

  /* Animated typing cursor */
  .cursor {
    display: inline-block;
    width: 2px;
    height: 1em;
    background: var(--violet);
    margin-left: 4px;
    animation: blink 1s step-end infinite;
    vertical-align: text-bottom;
  }
  @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

  /* Scroll reveal helper */
  [data-reveal] {
    opacity: 0;
    transform: translateY(20px);
    transition: opacity 0.7s ease, transform 0.7s ease;
  }
  [data-reveal].visible {
    opacity: 1;
    transform: translateY(0);
  }

  @media (max-width: 640px) {
    .screens-grid { grid-template-columns: 1fr 1fr; }
    .feature-grid { grid-template-columns: 1fr; }
    .tech-grid { grid-template-columns: repeat(2, 1fr); }
  }
</style>
</head>
<body>

<div class="orb orb-1"></div>
<div class="orb orb-2"></div>
<div class="orb orb-3"></div>

<div class="container">

  <!-- HERO -->
  <section class="hero">
    <div class="logo-ring">🎓</div>
    <h1>Nexus One</h1>
    <p class="hero-subtitle">KYAU University · Intelligent Attendance Management System</p>
    <p class="hero-desc">
      A full-stack university management platform for <strong>Khwaja Yunus Ali University</strong> — unifying student portals, faculty tools, attendance, finance, and career resources into one intelligent system.
    </p>
    <div class="badges">
      <span class="badge badge-java">☕ Java</span>
      <span class="badge badge-spring">🍃 Spring Boot</span>
      <span class="badge badge-mysql">📄 TXT File Storage</span>
      <span class="badge badge-thyme">🌿 Thymeleaf</span>
      <span class="badge badge-kyau">🏫 KYAU</span>
      <span class="badge badge-2026">📅 2026</span>
    </div>
  </section>

  <div class="divider"></div>

  <!-- TECH STACK -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">⚙️</div>
      <h2>Tech Stack</h2>
    </div>
    <div class="tech-grid">
      <div class="tech-card">
        <span class="tech-icon">☕</span>
        <div><div class="tech-name">Java 17</div><div class="tech-role">Core language</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">🍃</span>
        <div><div class="tech-name">Spring Boot</div><div class="tech-role">Backend framework</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">🔐</span>
        <div><div class="tech-name">Spring Security</div><div class="tech-role">Auth & roles</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">📄</span>
        <div><div class="tech-name">TXT File Storage</div><div class="tech-role">Flat file database</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">🌿</span>
        <div><div class="tech-name">Thymeleaf</div><div class="tech-role">Templating engine</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">📧</span>
        <div><div class="tech-name">JavaMail</div><div class="tech-role">Email / OTP</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">🗂️</span>
        <div><div class="tech-name">Java File I/O</div><div class="tech-role">Read / Write TXT</div></div>
      </div>
      <div class="tech-card">
        <span class="tech-icon">🐳</span>
        <div><div class="tech-name">Maven</div><div class="tech-role">Build tool</div></div>
      </div>
    </div>
  </section>

  <div class="divider"></div>

  <!-- UML DIAGRAM -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">🗺️</div>
      <h2>System Architecture (UML)</h2>
    </div>
    <div class="uml-container">
      <div class="uml-title">📐 Use Case + Component Overview</div>
      <div class="uml-svg-wrap">
        <svg viewBox="0 0 860 620" xmlns="http://www.w3.org/2000/svg" width="100%" style="max-width:860px; font-family:'JetBrains Mono',monospace;">
          <defs>
            <linearGradient id="gViolet" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#7C3AED"/>
              <stop offset="100%" stop-color="#4F46E5"/>
            </linearGradient>
            <linearGradient id="gCyan" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#06B6D4"/>
              <stop offset="100%" stop-color="#3B82F6"/>
            </linearGradient>
            <linearGradient id="gGreen" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#10B981"/>
              <stop offset="100%" stop-color="#059669"/>
            </linearGradient>
            <linearGradient id="gRed" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#EF4444"/>
              <stop offset="100%" stop-color="#DC2626"/>
            </linearGradient>
            <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 Z" fill="rgba(124,58,237,0.6)"/>
            </marker>
            <filter id="glow">
              <feGaussianBlur stdDeviation="3" result="blur"/>
              <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>
          </defs>

          <!-- Background -->
          <rect width="860" height="620" fill="#0F0D1A" rx="16"/>

          <!-- System boundary -->
          <rect x="180" y="40" width="500" height="540" rx="12" fill="none" stroke="rgba(124,58,237,0.3)" stroke-width="1.5" stroke-dasharray="6,4"/>
          <text x="430" y="32" fill="rgba(124,58,237,0.7)" font-size="11" text-anchor="middle" letter-spacing="2">«system» NEXUS ONE</text>

          <!-- ACTORS -->
          <!-- Admin -->
          <circle cx="60" cy="160" r="20" fill="url(#gRed)" filter="url(#glow)"/>
          <line x1="60" y1="180" x2="60" y2="220" stroke="#FCA5A5" stroke-width="2"/>
          <line x1="40" y1="200" x2="80" y2="200" stroke="#FCA5A5" stroke-width="2"/>
          <line x1="60" y1="220" x2="40" y2="248" stroke="#FCA5A5" stroke-width="2"/>
          <line x1="60" y1="220" x2="80" y2="248" stroke="#FCA5A5" stroke-width="2"/>
          <text x="60" y="160" fill="white" font-size="10" text-anchor="middle" dy="4">A</text>
          <text x="60" y="265" fill="#FCA5A5" font-size="11" text-anchor="middle" font-weight="700">Admin</text>

          <!-- Teacher -->
          <circle cx="60" cy="370" r="20" fill="url(#gGreen)" filter="url(#glow)"/>
          <line x1="60" y1="390" x2="60" y2="430" stroke="#6EE7B7" stroke-width="2"/>
          <line x1="40" y1="410" x2="80" y2="410" stroke="#6EE7B7" stroke-width="2"/>
          <line x1="60" y1="430" x2="40" y2="458" stroke="#6EE7B7" stroke-width="2"/>
          <line x1="60" y1="430" x2="80" y2="458" stroke="#6EE7B7" stroke-width="2"/>
          <text x="60" y="370" fill="white" font-size="10" text-anchor="middle" dy="4">T</text>
          <text x="60" y="475" fill="#6EE7B7" font-size="11" text-anchor="middle" font-weight="700">Teacher</text>

          <!-- Student -->
          <circle cx="800" cy="280" r="20" fill="url(#gViolet)" filter="url(#glow)"/>
          <line x1="800" y1="300" x2="800" y2="340" stroke="#C4B5FD" stroke-width="2"/>
          <line x1="780" y1="320" x2="820" y2="320" stroke="#C4B5FD" stroke-width="2"/>
          <line x1="800" y1="340" x2="780" y2="368" stroke="#C4B5FD" stroke-width="2"/>
          <line x1="800" y1="340" x2="820" y2="368" stroke="#C4B5FD" stroke-width="2"/>
          <text x="800" y="280" fill="white" font-size="10" text-anchor="middle" dy="4">S</text>
          <text x="800" y="385" fill="#C4B5FD" font-size="11" text-anchor="middle" font-weight="700">Student</text>

          <!-- USE CASES - Admin column -->
          <ellipse cx="310" cy="100" rx="110" ry="22" fill="rgba(239,68,68,0.12)" stroke="rgba(239,68,68,0.5)" stroke-width="1.5"/>
          <text x="310" y="104" fill="#FCA5A5" font-size="10" text-anchor="middle">Manage Users & Roles</text>

          <ellipse cx="310" cy="160" rx="110" ry="22" fill="rgba(239,68,68,0.12)" stroke="rgba(239,68,68,0.5)" stroke-width="1.5"/>
          <text x="310" y="164" fill="#FCA5A5" font-size="10" text-anchor="middle">Manage Finance & Fees</text>

          <ellipse cx="310" cy="220" rx="110" ry="22" fill="rgba(239,68,68,0.12)" stroke="rgba(239,68,68,0.5)" stroke-width="1.5"/>
          <text x="310" y="224" fill="#FCA5A5" font-size="10" text-anchor="middle">Post Notices</text>

          <!-- USE CASES - Teacher column -->
          <ellipse cx="310" cy="310" rx="110" ry="22" fill="rgba(16,185,129,0.12)" stroke="rgba(16,185,129,0.4)" stroke-width="1.5"/>
          <text x="310" y="314" fill="#6EE7B7" font-size="10" text-anchor="middle">Mark Attendance</text>

          <ellipse cx="310" cy="370" rx="110" ry="22" fill="rgba(16,185,129,0.12)" stroke="rgba(16,185,129,0.4)" stroke-width="1.5"/>
          <text x="310" y="374" fill="#6EE7B7" font-size="10" text-anchor="middle">Create Assignments</text>

          <ellipse cx="310" cy="430" rx="110" ry="22" fill="rgba(16,185,129,0.12)" stroke="rgba(16,185,129,0.4)" stroke-width="1.5"/>
          <text x="310" y="434" fill="#6EE7B7" font-size="10" text-anchor="middle">Faculty Sync Slots</text>

          <ellipse cx="310" cy="490" rx="110" ry="22" fill="rgba(16,185,129,0.12)" stroke="rgba(16,185,129,0.4)" stroke-width="1.5"/>
          <text x="310" y="494" fill="#6EE7B7" font-size="10" text-anchor="middle">Approve Leave Requests</text>

          <!-- USE CASES - Student column -->
          <ellipse cx="560" cy="130" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="134" fill="#C4B5FD" font-size="10" text-anchor="middle">View Dashboard & GPA</text>

          <ellipse cx="560" cy="190" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="194" fill="#C4B5FD" font-size="10" text-anchor="middle">Submit Assignments</text>

          <ellipse cx="560" cy="250" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="254" fill="#C4B5FD" font-size="10" text-anchor="middle">Generate Payment Slip</text>

          <ellipse cx="560" cy="310" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="314" fill="#C4B5FD" font-size="10" text-anchor="middle">Apply Leave Request</text>

          <ellipse cx="560" cy="370" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="374" fill="#C4B5FD" font-size="10" text-anchor="middle">Blood Finder</text>

          <ellipse cx="560" cy="430" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="434" fill="#C4B5FD" font-size="10" text-anchor="middle">Career Hub / Jobs</text>

          <ellipse cx="560" cy="490" rx="110" ry="22" fill="rgba(124,58,237,0.15)" stroke="rgba(124,58,237,0.5)" stroke-width="1.5"/>
          <text x="560" y="494" fill="#C4B5FD" font-size="10" text-anchor="middle">Faculty Sync Request</text>

          <!-- ARROWS Admin → use cases -->
          <line x1="80" y1="155" x2="196" y2="107" stroke="rgba(239,68,68,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="80" y1="163" x2="196" y2="162" stroke="rgba(239,68,68,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="80" y1="170" x2="196" y2="218" stroke="rgba(239,68,68,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>

          <!-- ARROWS Teacher → use cases -->
          <line x1="80" y1="365" x2="196" y2="312" stroke="rgba(16,185,129,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="80" y1="370" x2="196" y2="370" stroke="rgba(16,185,129,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="80" y1="376" x2="196" y2="428" stroke="rgba(16,185,129,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="80" y1="380" x2="196" y2="488" stroke="rgba(16,185,129,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>

          <!-- ARROWS Student → use cases -->
          <line x1="782" y1="273" x2="674" y2="135" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="276" x2="674" y2="194" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="280" x2="674" y2="252" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="284" x2="674" y2="312" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="288" x2="674" y2="372" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="292" x2="674" y2="432" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>
          <line x1="782" y1="296" x2="674" y2="492" stroke="rgba(124,58,237,0.5)" stroke-width="1.5" marker-end="url(#arrow)" stroke-dasharray="4,3"/>

          <!-- Legend -->
          <rect x="190" y="560" width="480" height="40" rx="8" fill="rgba(0,0,0,0.3)" stroke="rgba(124,58,237,0.2)" stroke-width="1"/>
          <ellipse cx="220" cy="580" rx="20" ry="8" fill="rgba(239,68,68,0.2)" stroke="rgba(239,68,68,0.5)" stroke-width="1"/>
          <text x="248" y="584" fill="#FCA5A5" font-size="9">Admin Actions</text>
          <ellipse cx="330" cy="580" rx="20" ry="8" fill="rgba(16,185,129,0.2)" stroke="rgba(16,185,129,0.5)" stroke-width="1"/>
          <text x="358" y="584" fill="#6EE7B7" font-size="9">Teacher Actions</text>
          <ellipse cx="455" cy="580" rx="20" ry="8" fill="rgba(124,58,237,0.2)" stroke="rgba(124,58,237,0.5)" stroke-width="1"/>
          <text x="483" y="584" fill="#C4B5FD" font-size="9">Student Actions</text>
          <line x1="570" y1="580" x2="598" y2="580" stroke="rgba(124,58,237,0.6)" stroke-width="1.5" stroke-dasharray="4,2"/>
          <text x="605" y="584" fill="#8B7FBB" font-size="9">Association</text>
        </svg>
      </div>
    </div>
  </section>

  <div class="divider"></div>

  <!-- FEATURES -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">✨</div>
      <h2>Core Features</h2>
    </div>
    <div class="feature-grid">

      <div class="feature-card" style="--accent: linear-gradient(90deg,#7C3AED,#4F46E5)">
        <span class="feature-emoji">📊</span>
        <div class="feature-title">Student Dashboard</div>
        <ul class="feature-list">
          <li>CGPA tracker with goal ring</li>
          <li>Nexus Intelligence alerts</li>
          <li>Attendance % at-a-glance</li>
          <li>Latest notices from authority</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#10B981,#06B6D4)">
        <span class="feature-emoji">✅</span>
        <div class="feature-title">Attendance System</div>
        <ul class="feature-list">
          <li>Teacher marks per session</li>
          <li>Course / Batch / Dept filters</li>
          <li>Auto attendance rate calc</li>
          <li>Low-attendance AI warning</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#F59E0B,#EF4444)">
        <span class="feature-emoji">📝</span>
        <div class="feature-title">Assignments</div>
        <ul class="feature-list">
          <li>Teacher creates with deadline</li>
          <li>Students attach & submit files</li>
          <li>Approve / reject submissions</li>
          <li>Overdue tracking & alerts</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#EC4899,#8B5CF6)">
        <span class="feature-emoji">🎓</span>
        <div class="feature-title">Academic Results</div>
        <ul class="feature-list">
          <li>Semester-wise transcript view</li>
          <li>CGPA goal tracker (3.5 goal)</li>
          <li>Performance forecast (AI)</li>
          <li>Download transcript PDF</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#06B6D4,#3B82F6)">
        <span class="feature-emoji">💳</span>
        <div class="feature-title">Finance & Fees</div>
        <ul class="feature-list">
          <li>Month-wise payment slip gen</li>
          <li>Live 3-copy preview (Trust Bank)</li>
          <li>Email notification on generate</li>
          <li>Sequential month enforcement</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#10B981,#059669)">
        <span class="feature-emoji">📋</span>
        <div class="feature-title">Leave Application</div>
        <ul class="feature-list">
          <li>Students submit with reason</li>
          <li>Teacher approve / reject</li>
          <li>Status: Pending/Approved/Rejected</li>
          <li>Application history view</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#8B5CF6,#EC4899)">
        <span class="feature-emoji">🤝</span>
        <div class="feature-title">Faculty Sync</div>
        <ul class="feature-list">
          <li>Book consultation slots</li>
          <li>Office hours management</li>
          <li>Chat / Meet integration</li>
          <li>Medium priority urgency tags</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#EF4444,#DC2626)">
        <span class="feature-emoji">🩸</span>
        <div class="feature-title">Blood Finder</div>
        <ul class="feature-list">
          <li>35+ donor directory</li>
          <li>Available Today toggle</li>
          <li>Broadcast Emergency alerts</li>
          <li>Filter by blood group & area</li>
        </ul>
      </div>

      <div class="feature-card" style="--accent: linear-gradient(90deg,#F97316,#F59E0B)">
        <span class="feature-emoji">💼</span>
        <div class="feature-title">Career Hub</div>
        <ul class="feature-list">
          <li>Official job circulars</li>
          <li>Student tuition postings</li>
          <li>Post & apply for jobs</li>
          <li>CV/Resume analyzer (AI)</li>
        </ul>
      </div>

    </div>
  </section>

  <div class="divider"></div>

  <!-- ROLES -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">👤</div>
      <h2>User Roles</h2>
    </div>
    <div class="uml-container">
      <table class="roles-table">
        <thead>
          <tr>
            <th>Role</th>
            <th>Portal</th>
            <th>Key Capabilities</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><span class="role-tag tag-admin">🔴 Admin</span></td>
            <td>University Admin Portal</td>
            <td>User management · Finance setup · Notice board · Full control</td>
          </tr>
          <tr>
            <td><span class="role-tag tag-teacher">🟢 Teacher</span></td>
            <td>Teacher Portal</td>
            <td>Mark attendance · Create assignments · Manage leaves · Faculty Sync · Career Hub · Blood Finder</td>
          </tr>
          <tr>
            <td><span class="role-tag tag-student">🟣 Student</span></td>
            <td>Student Portal</td>
            <td>Dashboard · Results · Finance slips · Leave requests · Assignments · Blood Finder · Career Hub</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <div class="divider"></div>

  <!-- SCREENS -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">🖼️</div>
      <h2>App Screens</h2>
    </div>
    <div class="screens-grid">
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#3730A3,#4F46E5)">🔐</div>
        <div class="screen-label">Login / Password Recovery</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#5B21B6,#7C3AED)">📊</div>
        <div class="screen-label">Student Dashboard</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#0F766E,#06B6D4)">✅</div>
        <div class="screen-label">Mark Attendance</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#92400E,#F59E0B)">💳</div>
        <div class="screen-label">Finance & Payment Slip</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#1D4ED8,#3B82F6)">🎓</div>
        <div class="screen-label">Semester Results</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#BE123C,#EF4444)">🩸</div>
        <div class="screen-label">Blood Finder</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#6D28D9,#EC4899)">🤝</div>
        <div class="screen-label">Faculty Sync</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#065F46,#10B981)">📝</div>
        <div class="screen-label">Assignments</div>
      </div>
      <div class="screen-card">
        <div class="screen-thumb" style="background: linear-gradient(135deg,#7C2D12,#F97316)">💼</div>
        <div class="screen-label">Career Hub</div>
      </div>
    </div>
  </section>

  <div class="divider"></div>

  <!-- SETUP -->
  <section data-reveal>
    <div class="section-title">
      <div class="section-icon">🚀</div>
      <h2>Getting Started</h2>
    </div>
    <div class="steps">
      <div class="step">
        <div class="step-num">1</div>
        <div class="step-content">
          <h4>Clone the repository</h4>
          <code>git clone https://github.com/YOUR_USERNAME/nexus-one.git
cd nexus-one</code>
        </div>
      </div>
      <div class="step">
        <div class="step-num">2</div>
        <div class="step-content">
          <h4>Configure data directory (TXT files)</h4>
          <code># application.properties
data.path=src/main/resources/data/
# All data stored as .txt flat files
# e.g. users.txt, attendance.txt, fees.txt</code>
        </div>
      </div>
      <div class="step">
        <div class="step-num">3</div>
        <div class="step-content">
          <h4>Configure email (OTP / Notifications)</h4>
          <code>spring.mail.host=smtp.gmail.com
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD</code>
        </div>
      </div>
      <div class="step">
        <div class="step-num">4</div>
        <div class="step-content">
          <h4>Build & run with Maven</h4>
          <code>mvn clean install
mvn spring-boot:run</code>
        </div>
      </div>
      <div class="step">
        <div class="step-num">5</div>
        <div class="step-content">
          <h4>Open in browser</h4>
          <code>http://localhost:8080/login</code>
        </div>
      </div>
    </div>
  </section>

  <div class="divider"></div>

  <!-- FOOTER -->
  <footer class="footer" data-reveal>
    <div class="footer-logo">Nexus One 🎓</div>
    <p>Built with ❤️ for Khwaja Yunus Ali University · CSE Department · 2026</p>
    <div class="footer-links">
      <a href="#" class="footer-link">📂 Source Code</a>
      <a href="#" class="footer-link">📹 Demo Video</a>
      <a href="#" class="footer-link">📄 Documentation</a>
      <a href="#" class="footer-link">🐛 Issues</a>
    </div>
    <p style="margin-top:8px; font-size:0.7rem;">© 2026 Nexus One · Khwaja Yunus Ali University · All rights reserved</p>
  </footer>

</div>

<script>
  // Scroll reveal
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        e.target.classList.add('visible');
        observer.unobserve(e.target);
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('[data-reveal]').forEach(el => observer.observe(el));

  // Stagger child cards on reveal
  const cardObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        const cards = e.target.querySelectorAll('.tech-card, .feature-card, .screen-card, .step');
        cards.forEach((card, i) => {
          card.style.animationDelay = `${i * 0.07}s`;
          card.style.animationFillMode = 'both';
        });
      }
    });
  }, { threshold: 0.05 });

  document.querySelectorAll('section').forEach(s => cardObserver.observe(s));
</script>
</body>
</html>
