<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <p>
      欢迎使用
      <a href="https://v3.vuejs.org/" target="_blank" rel="noopener">Vue 3</a> +
      <a href="https://cli.vuejs.org/" target="_blank" rel="noopener"
        >Vue CLI</a
      >
      项目
    </p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  msg: string;
}>();
</script>

<!-- 添加 "scoped" 属性以仅在此组件中使用此CSS -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
