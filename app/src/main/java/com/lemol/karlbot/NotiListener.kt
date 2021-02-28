package com.lemol.karlbot

import android.app.Notification
import android.app.Notification.Action
import android.app.NotificationManager
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

    private val client = OkHttpClient()

    // 고정
    private val tax = 0.3F // percent
    private val fee = 0.015F // percent
    private val interest = 0.5F // percent

    //변동
    var question = "오락실을 지키는 수호신 용 두 마리는 뭘까요?\n" + "정답은 '/answer'를 입력하세요."
    var answer = "정답: 일인용과 이인용"
    var stockCode = "064850"
    var gameMode = false
    var quizMode = false
    var saved = ""
    var balance = 1000000F
    var stocks = 0
    var price = 0

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
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        // val chatroomName = extras.getCharSequence(Notification.EXTRA_SUB_TEXT) // null if PM
        val commArgv = text?.split(" ")
        val comm = commArgv?.get(0)

        // Log.d("NOTI_LISTENER", sbn.packageName + Notification.WearableExtender(sbn.notification).actions.toString())
        // Log.d("NOTI_LISTENER", activeNotifications.toString())

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
                                    "/quiz : 준비중\n" +
                                    "/dice : 랜덤 주사위\n" +
                                    "/naver : 네이버 검색\n" +
                                    "/gg : 구글 검색\n" +
                                    "/yt : 유투브 검색\n" +
                                    "/ddg : DuckDuckGo 검색\n" +
                                    "/save : 데이터 저장\n" +
                                    "/load : 데이터 불러오기\n" +
                                    "/add : a+b 덧셈 계산\n" +
                                    "/reset : 저장된 변수 리셋\n" +
                                    "/troll : ???")
                        }

                        "/game" -> {
                            if (!gameMode) {
                                stockAPI()
                                actionReply(action, "" +
                                        "초기자금 100만원으로 가상주식 게임을 시작합니다.\n" +
                                        "매매 : /buy, /sell\n" +
                                        "잔고 : /balance\n" +
                                        "시세 : /info\n" +
                                        "종료 : /quit")
                                gameMode = true
                            } else {
                                actionReply(action, "이미 게임이 실행중입니다.")
                            }
                        }

                        "/balance" -> {
                            if (gameMode) {
                                stockAPI()
                                var asset = balance + stocks * price
                                var won = moneyConvert(balance)
                                var max = (balance / (price * (1 + fee / 100))).toInt()
                                actionReply(action, "" +
                                        "계좌잔고 : $won\n" +
                                        "주식잔고 : ${stocks}주\n" +
                                        "현재시세 : ${moneyConvert(price.toFloat())}\n" +
                                        "총 자산 : ${moneyConvert(asset)}\n" +
                                        "최대 ${max}주 매수 가능")
                            }
                        }

                        "/info" -> {
                            if (gameMode) {
                                stockAPI()
                                actionReply(action, "" +
                                        "주당시세 : ${moneyConvert(price.toFloat())}\n" +
                                        "수수료 : ${fee}%\n" +
                                        "세금 : ${tax}% (매도시 적용)\n" +
                                        "금리 : ${interest}% (미적용)\n" +
                                        "명령 : /buy, /sell, /balance, /quit")
                            }
                        }

                        "/buy" -> {
                            if (gameMode) {
                                stockAPI()
                                var q = contents.substringAfter(" : /buy ").toIntOrNull()
                                if (q == null || price == 0 || q > (balance / (price * (1 + fee / 100))) || q <= 0) {
                                    actionReply(action, "입력인자 또는 네트워크 오류가 발생했습니다.")
                                } else {
                                    var paid = price * q * (1 + fee / 100) //수수료만 지불
                                    balance -= paid
                                    stocks += q
                                    actionReply(action, "${q}주를 매수합니다.\n(${moneyConvert(paid)} 지불)")
                                }
                            }
                        }

                        "/sell" -> {
                            if (gameMode) {
                                stockAPI()
                                var q = contents.substringAfter(" : /sell ").toIntOrNull()
                                if (q == null || price == 0 || q > stocks || q <= 0) {
                                    actionReply(action, "입력인자 또는 네트워크 오류가 발생했습니다.")
                                } else {
                                    var paid = price * q * (1 - fee / 100 - tax / 100)
                                    balance += paid //수수료+세금 지불
                                    stocks -= q
                                    actionReply(action, "${q}주를 매도합니다.\n(${moneyConvert(paid)} 입금)")
                                }
                            }
                        }

                        "/quit" -> {
                            if (gameMode) {
                                actionReply(action, "게임이 종료되면 변수를 초기화하여 처음부터 다시 시작합니다. " +
                                        "정말로 종료하시겠습니까?\n(종료: /!quit)")
                            }
                        }

                        "/!quit" -> {
                            if (gameMode) {
                                actionReply(action, "게임이 종료되었습니다. 변수를 초기화합니다.")
                                gameMode = false
                                balance = 1000000F
                                stocks = 0
                                price = 0
                            }
                        }

                        "/quiz" -> {
                            if (!quizMode) {
                                actionReply(action, question)
                                quizMode = true
                            }
                        }

                        "/answer" -> {
                            if (quizMode) {
                                actionReply(action, answer)
                                gameMode = false
                            }
                        }

                        "/dice" -> {
                            val dice = Math.random() * 6 + 1
                            actionReply(action, "주사위를 굴려 " + dice.toString()[0] +
                                    "이(가) 나왔습니다.")
                        }

                        "/naver" -> {
                            var q = contents.substringAfter(" : /naver ")
                            if (" : /naver" in q) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://search.naver.com/search.naver?query=$q"
                                )
                            }
                        }

                        "/gg" -> {
                            var q = contents.substringAfter(" : /gg ")
                            if (" : /gg" in q) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://www.google.com/search?q=$q"
                                )
                            }
                        }

                        "/yt" -> {
                            var q = contents.substringAfter(" : /yt ")
                            if (" : /yt" in q) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://www.youtube.com/results?search_query=$q"
                                )
                            }
                        }

                        "/ddg" -> {
                            var q = contents.substringAfter(" : /ddg ")
                            if (" : /ddg" in q) {
                                actionReply(action, "검색어 패러미터가 없습니다.")
                            } else {
                                q = java.net.URLEncoder.encode(q, "utf-8")
                                actionReply(action,
                                        "https://duckduckgo.com/?t=ffab&q=$q"
                                )
                            }
                        }

                        "/save" -> {
                            saved = contents.substringAfter(" : /save ")
                            if (" : /save" in saved) {
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
                            var q = contents.substringAfter(" : /say ")
                            if (" : /say" in q) {
                                actionReply(action, "패러미터가 없습니다.")
                            } else {
                                actionReply(action, q)
                            }
                        }

                        "/add" -> {
                            var q = contents.substringAfter(" : /add ")
                            var a = q.substringBefore("+").toIntOrNull()
                            var b = q.substringAfter("+").toIntOrNull()
                            if (!q.contains("+") || a == null || b == null) {
                                actionReply(action, "입력인자 오류가 발생했습니다.")
                            } else {
                                actionReply(action, "${a + b}")
                            }
                        }

                        "/reset" -> {
                            actionReply(action, "모든 변수가 초기화되었습니다.")
                            gameMode = false
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
        var format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return format.format(money)
    }

    private fun stockAPI () {
        var url = "https://polling.finance.naver.com/api/realtime?query=SERVICE_ITEM:$stockCode"
        val request = Request.Builder()
                .url(url)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                price = if (response.isSuccessful) {
                    val jsonData = response.body?.string().toString()
                    val Jobject = JSONObject(jsonData)
                    val result = Jobject.getJSONObject("result")
                    val areas = result.getJSONArray("areas")
                    val datas = areas.getJSONObject(0).getJSONArray("datas")
                    val nv = datas.getJSONObject(0).getInt("nv")
                    nv
                } else {
                    0
                }
            }
        })
    }
}