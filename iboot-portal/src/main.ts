import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import i18n from "./locales";
import "./styles/global.css";
import { permission } from "./directives/permission";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(i18n as any);
app.use(ElementPlus);
app.directive("permission", permission);

app.mount("#app");
