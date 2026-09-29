<template>
  <footer class="footer">
    <div class="container">
      <div class="main-content">
        <div class="brand-section">
          <div class="logo">
            <img :src="logoSrc" alt="比特就业课" />
          </div>
          <p class="brand-desc">让零代码开发变得更简单、更高效、更智能</p>
        </div>

        <div class="nav-section">
          <h4 class="section-title">关于我们</h4>
          <a href="#" class="link">关于比特</a>
        </div>

        <div class="contact-section">
          <h4 class="section-title">联系我们</h4>
          <div class="contact-row">
            <span class="icon phone-icon"></span>
            <span class="contact-text">029-88218621</span>
          </div>
        </div>

        <div class="qrcode-section">
          <img :src="qrcodeSrc" alt="联系我们二维码" class="qrcode" />
          <span class="qrcode-text">联系我们</span>
        </div>
      </div>

      <div class="divider"></div>

      <div class="copyright">
        <p>西安比特教育科技有限公司 Copyright@2025 陕ICP备18006225号</p>
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
  background-color: #1a1d2e;
  padding: 48px 0 24px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.main-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
  gap: 24px;
  flex-wrap: wrap;
}

.brand-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 220px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;

  img {
    width: 157px;
    height: 38px;
    object-fit: contain;
  }
}

.brand-desc {
  font-size: 14px;
  color: #666677;
  margin: 8px 0 0 0;
}

.nav-section,
.contact-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 160px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.link {
  font-size: 14px;
  color: #888899;
  text-decoration: none;
}

.link:hover {
  color: #ffffff;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.phone-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  background: #888899;
  mask: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M6.62 10.79a15.053 15.053 0 006.59 6.59l2.2-2.2a1 1 0 011.01-.24c1.12.37 2.33.57 3.58.57a1 1 0 011 1V21a1 1 0 01-1 1C10.07 22 2 13.93 2 3a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.46.57 3.59a1 1 0 01-.25 1.01l-2.2 2.19z"/></svg>') center / contain no-repeat;
}

.contact-text {
  font-size: 14px;
  color: #888899;
}

.qrcode-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 140px;
}

.qrcode {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  object-fit: cover;
}

.qrcode-text {
  font-size: 12px;
  color: #888899;
}

.divider {
  width: 100%;
  height: 1px;
  background-color: #2a2d3e;
  margin-bottom: 20px;
}

.copyright {
  text-align: center;

  p {
    margin: 0;
    font-size: 12px;
    color: #555566;
  }
}

@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
