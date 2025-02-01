잠금화면 위에 현재 날씨, 향후 날씨, 특정 시간대의 날씨, 메모장 뷰를 포함하는 액티비티를 띄워주는 어플리케이션 
Foreground service, notification channel, SQLite, SharedPreference , REST API

메인액티비티의 버튼을 통해 지역 정보 설정, 드롭다운 메뉴를 통해 특정 시간대 지정, 토글을 통해 포그라운드 서비스 실행 / 중지

백그라운드에서 화면 켜짐 액션(intent.ACTION_SCREEN_ON)에 broadcastReceiver가 반응하여 액티비티 실행 LockScreenActivity의 onDestory()에서 메모 정보 DB에 저장
