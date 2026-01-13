package com.rebirth.my.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisDao analysisDao;

    public int saveAnalysis(AnalysisVo vo) {
        return analysisDao.insertAnalysis(vo);
    }

    public List<AnalysisVo> getAnalysisList() {
        return analysisDao.selectAnalysisList();
    }
}
