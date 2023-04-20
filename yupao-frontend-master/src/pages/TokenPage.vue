<template>
  <van-grid :column-num="1">
    <van-grid-item v-for="value in 1" :key="value" :text="`您正在和${user.username}聊天`"/>
  </van-grid>

  <div id="content">
    <div v-for="token in Token">
      <div>
        <span id='mes_center' style="font-size:10px">{{ nowTime}}</span>
        <span id='mes_right' style="margin-right: 15%">{{ token }}</span>
        <img id='mes_right' :src="user.avatarUrl" style="width:50px;height:50px;margin-top: -9%"/>
        <br>
      </div>
    </div>

    <div v-for="get in Get">
      <div>
        <span id='mes_center' style="font-size:10px;margin-top:-1%">{{ nowTime }}</span>
        <img :src="user.avatarUrl" style="width:50px;height:50px;margin-bottom: -5%"/>
        <span style="margin-left: -0%">{{ get }}</span>
      </div>
      <br>
    </div>
  </div>


  <div id="input">
    <textarea v-model="text" type="text" placeholder="在此输入内容..." id="input_text"></textarea>
    <div style="clear:both"></div>
    <div style="padding: 12px">
      <van-button block type="primary" id="submit" @click="doSearchResult">发送</van-button>
    </div>

  </div>
  <van-cell-group>
  </van-cell-group>
</template>

<script setup>
import {useRoute} from "vue-router";
import {onMounted, ref} from "vue";

const route = useRoute();
const user = route.query;
let nowTime = ref(null);
let Token = ref([]);
let Get = ref([]);
const text = ref(null);
console.log(user)
const timestampToTime = (times) => {
  let time = times[1]
  let mdy = times[0]
  mdy = mdy.split('/')
  let month = parseInt(mdy[0]);
  let day = parseInt(mdy[1]);
  let year = parseInt(mdy[2])
  return year + '-' + month + '-' + day + ' ' + time
}
nowTime = timestampToTime(new Date().toLocaleString('en-US', {hour12: false}).split(" "));

//打印结果为：2022-8-31 11:08:34

onMounted(() => {

});



const doSearchResult = () => {

  const name = user.username;
  const ws = new WebSocket("ws://localhost:8080/api/chat");
  ws.onopen = () => {
    console.log("Socket 已连接");
  }

  if (text.value !== "") {
    Token.value.push(text.value);
  }

  /**
   * 接收数据
   * @param ev
   */
  ws.onmessage = (ev) => {
    console.log(ev)
    const data = ev.data;
    const parse = JSON.parse(data);
    console.log(parse.message)
    Get.value.push(parse.message);
  }

  /**
   **关闭连接时将聊天记录存储
   */
  ws.onclose = () => {
    sessionStorage.setItem(user.username, Token);
    console.log("Socket已关闭");
  }



  const json = {"toName": name, "message": text.value}
  console.log(json)
  console.log(JSON.stringify(json))

  ws.onerror = function () {
    alert("Socket发生了错误");
    //此时可以尝试刷新页面
  }

  /**
   * 发送对话
   */
  ws.addEventListener('open', function () {
    if (text.value !=="") {
      ws.send(JSON.stringify(json))
    }
    text.value = "";
  });
}

</script>

<style scoped>
#content {
  border: aquamarine 1px solid;
  width: 100%;
  height: 300px;
}

#input textarea {
  width: 100%;
  height: 80px;
}

#content {
  border: aquamarine 1px solid;
  width: 100%;
  height: 300px;

}

#input {
  margin-top: 20px;
  width: 100%;
  height: 200px;
}

#input input {
  width: 100%;
  height: 100px;
}

#input button {
  float: right;
}

#mes_center {
  float: right;
  width: 100%;
  text-align: center;
}

#mes_right {
  float: right;
  width: 100%;
  text-align: right;
}
</style>