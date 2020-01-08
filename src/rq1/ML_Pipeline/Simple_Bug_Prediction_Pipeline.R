install.packages("RWeka")
install.packages("stringr")
install.packages("partykit")
install.packages("caret")
install.packages("tidyverse")

library(stringr)
library(RWeka)
library(partykit)
library(caret)
library(tidyverse)
set.seed(4650)

#File path for the github repository. 
pathSet<-paste("E:/Masters/Software Maintainence/GIT_PROJECT/cloud-bug-prediction-asat/ML_Pipeline/Prediction/hadoop_training.csv",sep="")

#Read data
pathRead <-read.csv(pathSet)
print('path read dimensions before removing duplicates')
print(dim(pathRead))
#retrieve training set with no duplicates for both column "bug" and "name"
pathRead <-pathRead[!duplicated(pathRead[c("name","bug")]),]
#print('path read dimensions after removing duplicates')
#print(dim(pathRead))

#Get the duplicates ofr column name class.
duplicates <-pathRead[duplicated(pathRead[c("name")]),]
#get the class name removing all the duplicates.
pathRead <-pathRead[!duplicated(pathRead[c("name")]),]

#merge using bug column which ultimately get two columns related to bugs with yes and no.
table_new <- merge(x=pathRead, y=duplicates %>% select(name,  bug), by="name",all.x=TRUE)

table_new$bug <- ifelse(is.na(table_new$bug.y) ,table_new$bug.x, "yes")

table_new$bug <- ifelse(table_new$bug==1 ,"no", "yes")

trainIndex <- createDataPartition(table_new$bug, 
                                  p = .75, 
                                  list = FALSE, 
                                  times = 1)

trainingSet <- table_new[ trainIndex,]
trainingSet <- trainingSet[, -c(9:10)] 
print(trainingSetExtracted)
testSet  <- table_new[-trainIndex,]


#names(trainingSet)
#attach(trainingSet)
str(trainingSet)  
trainingSet <- as.data.frame(unclass(trainingSet))
str(trainingSet)
#logistic regression model
model <- Logistic(bug~loc+cbo+dit+wmc+lcom, data = trainingSet) #Logistic builds multinomial logistic regression models based on ridge estimation

# Use 10 fold cross-validation. - evaluate model using test data
e2 <- evaluate_Weka_classifier(model,newdata = testSet,cost = matrix(c(0,2,1,0), ncol = 2),numFolds = 10, complexity = TRUE,sed = 123, class = TRUE)
confusionMatrix<-e2$confusionMatrix

confusionMatrix

#precision doesnt mirror the inspection cost. need cost oriented models
precision <-confusionMatrix[2,2]/sum(confusionMatrix[,2])#PRECISION...
precision
recall<-confusionMatrix[2,2]/sum(confusionMatrix[2,])#Recall...
recall
accuracy<-sum(confusionMatrix[1,1],confusionMatrix[2,2])/sum(confusionMatrix[,])
accuracy
false_positive_rate <- confusionMatrix[2,1]/sum(confusionMatrix[1,])#false positive rate
false_positive_rate

