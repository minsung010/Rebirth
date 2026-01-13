package com.rebirth.my.config;

import com.rebirth.my.domain.EcoTodoTask;
import com.rebirth.my.mapper.EcoTodoMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EcoTodoMapper ecoTodoMapper;

    @Override
    public void run(String... args) throws Exception {
        if (ecoTodoMapper.countTasks() == 0) {
            System.out.println("Initializing Eco Missions...");
            List<EcoTodoTask> tasks = new ArrayList<>();

            // 1. Waste & Recycling
            addTask(tasks, "WASTE_01", "오늘 발생한 쓰레기에서 1개라도 정확한 분리배출 해보기", 10);
            addTask(tasks, "WASTE_02", "의류 소재(면/폴리에스터/모직) 확인하고 분리하여 세탁", 15);
            addTask(tasks, "WASTE_03", "버리려는 옷 1벌은 재활용 수거함에 분리", 20);
            addTask(tasks, "WASTE_04", "플라스틱 용기 깨끗이 헹구고 라벨 제거", 10);
            addTask(tasks, "WASTE_05", "종이·비닐 혼합 포장재 재질 별로 분리하기", 10);
            addTask(tasks, "WASTE_06", "배달 음식 주문 시 “수저·젓가락 제외” 옵션 선택", 10);
            addTask(tasks, "WASTE_07", "오늘 하루 일회용컵 대신 텀블러 사용", 20);
            addTask(tasks, "WASTE_08", "택배 박스를 오늘 하나라도 올바르게 접어서 배출", 10);

            // 2. Energy Saving
            addTask(tasks, "ENERGY_01", "사용하지 않는 방의 불 전부 끄기", 10);
            addTask(tasks, "ENERGY_02", "전자기기 충전 완료되면 충전기 바로 뽑기", 10);
            addTask(tasks, "ENERGY_03", "냉난방 대신 적정 실내온도 유지(20–22℃)", 15);
            addTask(tasks, "ENERGY_04", "엘리베이터 대신 계단 1번 이상 사용", 20);
            addTask(tasks, "ENERGY_05", "컴퓨터 화면 밝기 20% 낮추기", 10);
            addTask(tasks, "ENERGY_06", "불필요한 멀티탭 스위치 OFF 하기", 10);
            addTask(tasks, "ENERGY_07", "세탁 시 에코/절전 모드 한번 사용해보기", 15);
            addTask(tasks, "ENERGY_08", "외출 시 대기전력 차단 체크", 10);

            // 3. Lifestyle & Mobility
            addTask(tasks, "LIFE_01", "가까운 거리는 걷기", 20);
            addTask(tasks, "LIFE_02", "1회라도 대중교통 이용하기", 20);
            addTask(tasks, "LIFE_03", "1시간 중 10분은 휴대전화 사용 줄이고 휴식", 10);
            addTask(tasks, "LIFE_04", "오늘 구매할 물건 1개는 불필요하면 미루기(NO BUY)", 30);
            addTask(tasks, "LIFE_05", "외식 시 잔반 남기지 않기", 20);
            addTask(tasks, "LIFE_06", "장보기 시 비닐봉투 대신 장바구니 사용", 15);
            addTask(tasks, "LIFE_07", "음식물 쓰레기 발생량 기록해보기", 15);
            addTask(tasks, "LIFE_08", "종이 영수증 대신 전자영수증 요청", 10);

            for (EcoTodoTask task : tasks) {
                ecoTodoMapper.insertTask(task);
            }
            System.out.println("Eco Missions Initialized: " + tasks.size());
        }
    }

    private void addTask(List<EcoTodoTask> tasks, String code, String title, int points) {
        EcoTodoTask task = new EcoTodoTask();
        task.setId((long) (tasks.size() + 1));
        task.setCode(code);
        task.setTitle(title);
        task.setDefaultPoints(points);
        task.setIsActive("Y");
        task.setSortOrder(tasks.size() + 1);
        tasks.add(task);
    }
}
