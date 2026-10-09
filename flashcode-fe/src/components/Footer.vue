<template>
  <footer class="footer">
    <div class="container">
      <div class="main-content">
        <div class="brand-section">
          <div class="logo">
            <img :src="logoSrc" alt="Flashcode" />
          </div>
          <p class="brand-desc">让零代码开发变得更简单、更高效、更智能</p>
        </div>

        <div class="nav-section">
          <h4 class="section-title">关于我们</h4>
          <a href="/about" class="link">项目说明</a>
        </div>
      </div>

      <div class="copyright">
        <p>
          <span>Flashcode</span>
          <span class="sep" aria-hidden="true"></span>
          <span>Copyright © 2026</span>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
const logoSrc = new URL('@/imges/logoFooter.webp', import.meta.url).href;
</script>

<style scoped lang="scss">
.footer {
  width: 100%;
  background:
    radial-gradient(1200px 180px at 12% -40%, rgba(0, 164, 255, 0.16), transparent 60%),
    #16192a;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 56px 0 28px;
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 32px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
  column-gap: 72px;
  margin-bottom: 40px;
}

.brand-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 2px;
}

.logo img {
  display: block;
  width: 220px;
  height: 46px;
  object-fit: cover;
  object-position: center;
}

.brand-desc {
  max-width: 280px;
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.55);
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 6px;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #ffffff;
}

.link {
  font-size: 14px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.62);
  text-decoration: none;
  transition: color 0.2s ease;
}

.link:hover {
  color: #ffffff;
}

.copyright {
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;

  p {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px 0;
    margin: 0;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.42);
  }

  a {
    color: inherit;
    text-decoration: none;
  }

  a:hover {
    color: rgba(255, 255, 255, 0.72);
  }
}

.sep {
  width: 3px;
  height: 3px;
  margin: 0 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.28);
}

@media (max-width: 900px) {
  .footer {
    padding: 40px 0 24px;
  }

  .main-content {
    grid-template-columns: 1fr 1fr;
    row-gap: 32px;
    column-gap: 24px;
  }

  .brand-section {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .copyright p {
    flex-direction: column;
    gap: 6px;
  }

  .sep {
    display: none;
  }
}
</style>
