package me.domin.bilock;

public interface TrainContract {
    interface View{
        void changeNum(int fileNum);
        void finishTrain();

    }
    interface Presenter{
        void trainData(int type);
        void trainModel(int normal_type);
    }
}
