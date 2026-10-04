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
          <a href="#" class="link">关于比特</a>
        </div>

        <div class="contact-section">
          <h4 class="section-title">联系我们</h4>
          <a class="contact-row" href="tel:029-88218621">
            <span class="icon phone-icon"></span>
            <span class="contact-text">029-88218621</span>
          </a>
        </div>

        <div class="qrcode-section">
          <div class="qrcode-frame">
            <img :src="qrcodeSrc" alt="联系我们二维码" class="qrcode" />
          </div>
          <span class="qrcode-text">联系我们</span>
        </div>
      </div>

      <div class="copyright">
        <p>
          <span>西安比特教育科技有限公司</span>
          <span class="sep" aria-hidden="true"></span>
          <span>Copyright © 2025</span>
          <span class="sep" aria-hidden="true"></span>
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">陕ICP备18006225号</a>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
const logoSrc = new URL('@/imges/logoFooter.webp', import.meta.url).href;
const qrcodeSrc = new URL('@/imges/wxCode.webp', import.meta.url).href;
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
  grid-template-columns: 1fr auto auto auto;
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

.nav-section,
.contact-section {
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

.link,
.contact-row {
  font-size: 14px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.62);
  text-decoration: none;
  transition: color 0.2s ease;
}

.link:hover,
.contact-row:hover {
  color: #ffffff;
}

.contact-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
}

.phone-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  background: currentColor;
  mask: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M6.62 10.79a15.053 15.053 0 006.59 6.59l2.2-2.2a1 1 0 011.01-.24c1.12.37 2.33.57 3.58.57a1 1 0 011 1V21a1 1 0 01-1 1C10.07 22 2 13.93 2 3a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.46.57 3.59a1 1 0 01-.25 1.01l-2.2 2.19z"/></svg>') center / contain no-repeat;
}

.qrcode-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.qrcode-frame {
  padding: 8px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.qrcode {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
}

.qrcode-text {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.5);
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

  .brand-section,
  .qrcode-section {
    grid-column: 1 / -1;
  }

  .qrcode-section {
    align-items: flex-start;
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
