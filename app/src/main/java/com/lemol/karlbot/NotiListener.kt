package com.lemol.karlbot

import android.app.Notification
import android.app.Notification.Action
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.*


class NotiListener : NotificationListenerService() {

    // Issues
    // Need to differentiate users
    // Need to save data

    private val client = OkHttpClient()

    // 고정
    private val tax = 0.3F // percent
    private val fee = 0.015F // percent
    private val interest = 0.5F // percent

    //변동
    private var question = "오락실을 지키는 수호신 용 두 마리는?"
    private var answer = "일인용과 이인용"
    private var stockCode = "064850"
    private var quizMode = false
    private var saved = ""
    private var balance = 1000000F
    private var stocks = 0
    private var price = 0
    private var personalData : MutableMap<String, List<Number>> = mutableMapOf()
    // {chatRoomName=[balance(Float), stocks(Int), gameMode(Int)]}
    // gameMode is on if it's 1, it's off if it's 0 or null
    private var stocksData : MutableMap<String, List<Number>> = mutableMapOf()
//    var questionMap = mutableListOf(
//            mutableListOf("오락실을 지키는 수호신 용 두 마리는?", "일인용과 이인용"),
//            mutableListOf("귀가 불에 타면?", "타이어")
//    )

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "Started Karlbot Service", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Karlbot Service Not Running", Toast.LENGTH_SHORT).show()
    }

    // Executed when notification is received
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // Contents of the notification in such form: "sender : message"
        val contents = sbn!!.notification.tickerText.toString()

        // Can be useful later
        val extras = sbn.notification.extras
        // val sender = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        val chatRoomName = extras.getCharSequence(Notification.EXTRA_SUB_TEXT) // null if PM
        val commArgv = text.split(" ")
        val comm = commArgv[0]

        // Log.d("NOTI_LISTENER", sbn.packageName + Notification.WearableExtender(sbn.notification).actions.toString())
        // Log.d("NOTI_LISTENER", activeNotifications.toString())
        // Log.d("NOTI_LISTENER", extras.toString())

        if (sbn.packageName.equals("com.kakao.talk")) {

            // Code below may work on later versions of Android, but doesn't work on 6.0.1
            // This is because direct actions were introduced with Android version 7.0
            // Thus switching to Wearable Extender which comes with Wear OS by Google
            // Needs to be installed prior to usage
            // https://play.google.com/store/apps/details?id=com.google.android.wearable.app


            val noti = Notification.WearableExtender(sbn.notification).actions

            // Check for Android API level 24 (Android 7.0) or above
            // if (Build.VERSION.SDK_INT >= 24){
            //     var noti = sbn!!.notification.actions
            // }

            // This loop seeks for possible actions within the notification
            for (action in noti) {
                if (!action.remoteInputs.isNullOrEmpty() &&
                        (action.title.toString().contains("reply", true) ||
                        action.title.toString().contains("답장"))) {
                    when (comm) {
                        "/start" -> {
                            actionReply(action, "" +
                                    "등록된 명령어:\n" +
                                    "/start : 이 메시지 출력\n" +
                                    "/say : 말시키기\n" +
                                    "/game : 가상주식 게임\n" +
                                    "/quiz : (준비중)\n" +
                                    "/psycho : 성격유형 검사\n" +
                                    "/dice : 랜덤 주사위\n" +
                                    "/naver : 네이버 검색\n" +
                                    "/gg : 구글 검색\n" +
                                    "/yt : 유투브 검색\n" +
                                    "/ddg : DuckDuckGo 검색\n" +
                                    "/save : 데이터 저장\n" +
                                    "/load : 데이터 불러오기\n" +
                                    "/add : a+b 덧셈 계산(beta)\n" +
                                    "/data : (준비중)\n" +
                                    "/reset : 저장된 변수 리셋\n" +
                                    "/troll : ???")
                        }

                        "/game" -> {
                            // used to be !gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 0) {
                                stockAPI()
                                stocksData[chatRoomName.toString()] = listOf(1000000F, 0, 1)
                                actionReply(action, "" +
                                        "초기자금 100만원으로 가상주식 게임을 시작합니다.\n" +
                                        "매매 : /buy, /sell\n" +
                                        "잔고 : /balance\n" +
                                        "시세 : /info\n" +
                                        "종료 : /quit")
                            } else {
                                actionReply(action, "이미 게임이 실행중입니다.")
                            }
                        }

                        "/balance" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                stockAPI()
                                balance = stocksData[chatRoomName.toString()]?.get(0) as Float
                                stocks = stocksData[chatRoomName.toString()]?.get(1) as Int
                                val asset = balance + stocks * (price * (1 - fee/100 - tax/100))
                                val won = moneyConvert(balance)
                                val max = (balance / (price * (1 + fee/100))).toInt()
                                val sise = if (price == 0) { "네트워크 오류" } else { moneyConvert(price.toFloat()) }
                                actionReply(action, "" +
                                        "계좌잔고 : $won\n" +
                                        "주식잔고 : ${stocks}주\n" +
                                        "현재시세 : $sise\n" +
                                        "총 자산 : moneyConvert(asset)\n" +
                                        "최대 ${max}주 매수 가능")
                            }
                        }

                        "/info" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                stockAPI()
                                val sise = if (price == 0) { "네트워크 오류" } else { moneyConvert(price.toFloat()) }
                                actionReply(action, "" +
                                        "주당시가 : $sise\n" +
                                        "수수료 : ${fee}%\n" +
                                        "세금 : ${tax}% (매도시 적용)\n" +
                                        "금리 : ${interest}% (미적용)\n" +
                                        "명령 : /buy, /sell, /balance, /quit")
                            }
                        }

                        "/buy" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                stockAPI()
                                balance = stocksData[chatRoomName.toString()]?.get(0) as Float
                                stocks = stocksData[chatRoomName.toString()]?.get(1) as Int
                                val amount = commArgv.getOrNull(1)?.toIntOrNull()
                                if (amount == null || price == 0 || amount > (balance / (price * (1 + fee/100))) || amount <= 0) {
                                    actionReply(action, "입력인자 또는 네트워크 오류가 발생했습니다.")
                                } else {
                                    val paid = price * amount * (1 + fee/100) //수수료만 지불
                                    val minSell = price * (1 + fee/100) / (1 - fee/100 - tax/100)
                                    balance -= paid
                                    stocks += amount
                                    actionReply(action, "${moneyConvert(price.toFloat())}에 ${amount}주를 매수합니다.\n" +
                                            "- 총 ${moneyConvert(paid)} 지불\n" +
                                            "- 매도 시세 ${moneyConvert(minSell)} 초과시 이윤")
                                    stocksData[chatRoomName.toString()] = listOf(balance, stocks, 1)
                                }
                            }
                        }

                        "/sell" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                stockAPI()
                                balance = stocksData[chatRoomName.toString()]?.get(0) as Float
                                stocks = stocksData[chatRoomName.toString()]?.get(1) as Int
                                val amount = commArgv.getOrNull(1)?.toIntOrNull()
                                if (amount == null || price == 0 || amount > stocks || amount <= 0) {
                                    actionReply(action, "입력인자 또는 네트워크 오류가 발생했습니다.")
                                } else {
                                    val paid = price * amount * (1 - fee/100 - tax/100)
                                    val minBuy = price * (1 - fee/100 - tax/100) / (1 + fee/100)
                                    balance += paid //수수료+세금 지불
                                    stocks -= amount
                                    actionReply(action, "${moneyConvert(price.toFloat())}에 ${amount}주를 매도합니다.\n" +
                                            "- 총 ${moneyConvert(paid)} 입금\n" +
                                            "- 매수 시세 ${moneyConvert(minBuy)} 미만시 이윤")
                                    stocksData[chatRoomName.toString()] = listOf(balance, stocks, 1)
                                }
                            }
                        }

                        "/quit" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                actionReply(action, "게임이 종료되면 변수를 초기화하여 처음부터 다시 시작합니다. " +
                                        "정말로 종료하시겠습니까?\n(종료: /!quit)")
                            }
                        }

                        "/!quit" -> {
                            // used to be gameMode
                            if ((stocksData[chatRoomName.toString()]?.get(2))?:0 == 1) {
                                actionReply(action, "게임이 종료되었습니다. 변수를 초기화합니다.")
                                stocksData.remove(chatRoomName.toString())
                                balance = 1000000F
                                stocks = 0
                                price = 0
                            }
                        }

                        "/quiz" -> {
                            if (!quizMode) {
                                actionReply(action, "$question\n정답은 '/answer'를 입력하세요.")
                                quizMode = true
                            }
                        }

                        "/answer" -> {
                            if (quizMode) {
                                actionReply(action, "정답: $answer")
                                quizMode = false
                            }
                        }

                        "/psycho" -> {
                            val code = commArgv.getOrNull(1)?.toIntOrNull()
                            if (code == null) {
                                actionReply(action, "성격검사를 실행합니다. 다음 질문에 답해주시기 바랍니다.\n\n" +
                                        "▶1. 결정을 내리는 방식에 더 와닿는 갈등을 고르세요. (1/2)\n" +
                                        "① 내가 해야 하는 일 vs 타인에 대한 배려\n" +
                                        "② 내가 원하는 것 vs 다수를 위한 것\n\n" +
                                        "▶2. 주변환경을 인식할 때 더 와닿는 갈등을 고르세요. (1/2)\n" +
                                        "① 경험을 통해서 vs 주관을 통해서\n" +
                                        "② 다양한 가능성 vs 당장의 현실\n\n" +
                                        "▶3. 평상시 자신의 행동방침을 고르세요. (1/2/3/4)\n" +
                                        "① 새로운 자극을 찾아 나섬\n" +
                                        "② 정보를 관리하고 체계화\n" +
                                        "③ 사람들을 모아 추진\n" +
                                        "④ 묵묵히 적응\n\n" +
                                        "답안은 명령어 인자에 숫자로 적어주시기 바랍니다.\n" +
                                        "예: 답이 2번, 1번, 4번일 경우\n" +
                                        "'/psycho 214' 입력")
                            } else {
                                when (code) {
                                    111 -> { actionReply(action, "당신은 SeTi 타입입니다.\n" + //ESTP
                                            "전형적인 싸이코패스로서 의도하지 않게 주변 환경에 큰 피해를 입히며 " +
                                            "폭력적인 성향을 가지고 있습니다.") }
                                    112 -> { actionReply(action, "당신은 NiFe 타입입니다.\n" + //INFJ
                                            "전형적인 싸이코패스로서 주변 사람들을 교묘히 이용하며 설득시키는 " +
                                            "독재자의 성향을 가지고 있습니다.") }
                                    113 -> { actionReply(action, "당신은 FeNi 타입입니다.\n" + //ENFJ
                                            "전형적인 싸이코패스로서 사람들을 감정적으로 취약한 상태로 만들어 " +
                                            "이용하려는 성향을 가지고 있습니다.") }
                                    114 -> { actionReply(action, "당신은 TiSe 타입입니다.\n" + //ISTP
                                            "전형적인 싸이코패스로서 무자비하고 폭력적이며 자신의 이익만을 생각하는 " +
                                            "무법자 성향을 가지고 있습니다.") }
                                    121 -> { actionReply(action, "당신은 NeTi 타입입니다.\n" + //ENTP
                                            "전형적인 싸이코패스로서 거짓말을 잘 하고 자기중심적이며 인간관계에 있어 " +
                                            "파괴적인 성향을 가지고 있습니다.") }
                                    122 -> { actionReply(action, "당신은 SiFe 타입입니다.\n" + //ISFJ
                                            "전형적인 싸이코패스로서 사소한 규율이라도 어길 경우 과하게 형벌하려는 " +
                                            "악덕 관리자 성향을 가지고 있습니다.") }
                                    123 -> { actionReply(action, "당신은 FeSi 타입입니다.\n" + //ESFJ
                                            "전형적인 싸이코패스로서 흑색선전과 유언비어를 즐기고 사회적 지위를 얻기 " +
                                            "위해 타인들을 누르려는 성향을 가지고 있습니다.") }
                                    124 -> { actionReply(action, "당신은 TiNe 타입입니다.\n" + //INTP
                                            "전형적인 싸이코패스로서 타인들의 능력을 내리깔며 비웃고 도덕에 관심이 " +
                                            "없는 매드사이언티스트의 성향을 가지고 있습니다.") }
                                    211 -> { actionReply(action, "당신은 SeFi 타입입니다.\n" + //ESFP
                                            "전형적인 싸이코패스로서 충동적이고 폭력적이며 모두의 주목을 받기 위해 " +
                                            "타인들을 끌어내리려는 성향을 가지고 있습니다.") }
                                    212 -> { actionReply(action, "당신은 NiTe 타입입니다.\n" + //INTJ
                                            "전형적인 싸이코패스로서 공감능력이 부족하고 목표를 위해 누구든 희생시키는 " +
                                            "나르시시스트의 성향을 가지고 있습니다.") }
                                    213 -> { actionReply(action, "당신은 TeNi 타입입니다.\n" + //ENTJ
                                            "전형적인 싸이코패스로서 공격적이고 무슨 수를 써서라도 상대방을 " +
                                            "굴복시키려는 성향을 가지고 있습니다.") }
                                    214 -> { actionReply(action, "당신은 FiSe 타입입니다.\n" + //ISFP
                                            "전형적인 싸이코패스로서 자기중심적이고 충동적이며 타협될 수 없는 " +
                                            "소모적인 성향을 가지고 있습니다.") }
                                    221 -> { actionReply(action, "당신은 NeFi 타입입니다.\n" + //ENFP
                                            "전형적인 싸이코패스로서 체제에 반항적이고 교묘히 방해행위를 주도하는 " +
                                            "악동의 성향을 가지고 있습니다.") }
                                    222 -> { actionReply(action, "당신은 SiTe 타입입니다.\n" + //ISTJ
                                            "전형적인 싸이코패스로서 까다롭고 공감능력이 부족하며 극한의 절제를 강요하는 " +
                                            "극단주의적인 성향을 가지고 있습니다.") }
                                    223 -> { actionReply(action, "당신은 TeSi 타입입니다.\n" + //ESTJ
                                            "전형적인 싸이코패스로서 위협적이고 폭력적이며 자신의 방식을 강요하는 " +
                                            "조폭보스의 성향을 가지고 있습니다.") }
                                    224 -> { actionReply(action, "당신은 FiNe 타입입니다.\n" + //INFP
                                            "전형적인 싸이코패스로서 타인들의 감정을 조작하고 주변에 보이는 모든것을 " +
                                            "파괴시키고자 하는 성향을 가지고 있습니다.") }
                                }
                            }
                        }

                        "/dice" -> {
                            val dice = Math.random() * 6 + 1
                            actionReply(action, "주사위를 굴려 " + dice.toString()[0] +
                                    "이(가) 나왔습니다.")
                        }

                        "/naver" -> {
                            var q = text.substringAfter("/naver ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://search.naver.com/search.naver?query=$q"
                                )
                            }
                        }

                        "/gg" -> {
                            var q = text.substringAfter("/gg ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://www.google.com/search?q=$q"
                                )
                            }
                        }

                        "/yt" -> {
                            var q = text.substringAfter("/yt ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://www.youtube.com/results?search_query=$q"
                                )
                            }
                        }

                        "/ddg" -> {
                            var q = text.substringAfter("/ddg ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://duckduckgo.com/?t=ffab&q=$q"
                                )
                            }
                        }

                        "/save" -> {
                            saved = text.substringAfter("/save ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "데이터를 저장할 수 없습니다.")
                            } else {
                                actionReply(action,
                                        "'$saved'\n위 데이터를 저장했습니다. 불러오려면 /load를 입력하세요."
                                )
                            }
                        }

                        "/load" -> {
                            if (saved == "") {
                                actionReply(action, "저장된 데이터가 없습니다.")
                            } else {
                                actionReply(action,
                                        "저장된 데이터는 다음과 같습니다:\n$saved"
                                )
                            }
                        }

                        "/say" -> {
                            val q = text.substringAfter("/say ")
                            if (commArgv.getOrNull(1).isNullOrBlank()) {
                                actionReply(action, "패러미터가 없습니다.")
                            } else {
                                actionReply(action, q)
                            }
                        }

                        "/add" -> {
                            val q = text.substringAfter("/add ")
                            val a = q.substringBefore("+").toIntOrNull()
                            val b = q.substringAfter("+").toIntOrNull()
                            if (!q.contains("+") || a == null || b == null) {
                                actionReply(action, "입력인자 오류가 발생했습니다.")
                            } else {
                                actionReply(action, "${a + b}")
                            }
                        }

                        "/data" -> {
                            var q = text.substringAfter("/data ")
                            //actionReply(action, "${personalData[sender]?.get(0)?.toString()}")
                            actionReply(action, "$stocksData")
                            //actionReply(action, "$personalData")
                        }

                        "/reset" -> {
                            actionReply(action, "모든 변수가 초기화되었습니다.")
                            quizMode = false
                            saved = ""
                            balance = 1000000F
                            stocks = 0
                            price = 0
                        }

                        "/troll" -> {
                            actionReply(action, "" +
                                    "....................../´¯/)  \n" +
                                    "....................,/¯../  \n" +
                                    ".................../..../  \n" +
                                    "............./´¯/'...'/´¯¯`·¸  \n" +
                                    "........../'/.../..../......./¨¯\\  \n" +
                                    "........('(...´...´.... ¯~/'...')  \n" +
                                    ".........\\.................'...../  \n" +
                                    "..........''...\\.......... _.·´  \n" +
                                    "............\\..............(  \n" +
                                    "..............\\.............\\...\n" +
                                    "Fahk you azz hore")
                        }
                    }
                }
            }
        }
        else {
            // If the notification is from other source
            // Log.d("NOTI_LISTENER", "(Other) $contents");
        }
    }

    // This method replies to the notification
    // action is the action from the notification, msg is the content of the message to be sent
    // The app cannot interact with chats on its own since the app does not use KakaoTalk API
    // Thus the app requires to have received a notification before sending any messages
    private fun actionReply(action: Action, msg: String) {
        val intent = Intent()
        val bundle = Bundle()
        for (input in action.remoteInputs) bundle.putCharSequence(input.resultKey, msg)
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        action.actionIntent.send(this, 0, intent)
    }

    fun genQuiz() {

    }

    private fun moneyConvert(money: Float): String {
        val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return format.format(money)
    }

    private fun stockAPI () {
        val url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:$stockCode"
        val request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                price = 0
            }
            override fun onResponse(call: Call, response: Response) {
                price = if (response.isSuccessful) {
                    val jsonDataF = response.body?.string().toString()
                    val jsonObjectF = JSONObject(jsonDataF)
                    val result = jsonObjectF.getJSONObject("result")
                    val areas = result.getJSONArray("areas")
                    val dataF = areas.getJSONObject(0).getJSONArray("datas")
                    val nv = dataF.getJSONObject(0).getInt("nv")
                    nv
                } else {
                    0
                }
            }
        })
    }
}