provider "aws" {
  region = "us-west-2"
}

# Public ECR Repository already set up

# IAM roles for GitHub Actions already set up

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# ECS security group
resource "aws_security_group" "ecs_security_group" {
  name        = "ecs-security-group"
  description = "Allow inbound traffic to the ECS task"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Allow app traffic from the internet"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# RDS security group
resource "aws_security_group" "rds_security_group" {
  name        = "rds-security-group"
  description = "Allow PostgreSQL access only from ECS"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Allow PostgreSQL from ECS tasks"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_security_group.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier          = "payment-db"
  db_name             = var.db_name
  engine              = "postgres"
  instance_class      = "db.t4g.micro"
  allocated_storage   = 20
  username            = var.db_username
  password            = var.db_password
  publicly_accessible = false
  vpc_security_group_ids = [aws_security_group.rds_security_group.id]
  skip_final_snapshot = true
}


# ECS Cluster
resource "aws_ecs_cluster" "app_cluster" {
  name = "payment-cluster"
}


# ECS task execution role
data "aws_iam_role" "ecs_task_execution_role" {
  name = "ecsTaskExecutionRole"
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = data.aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}


# CloudWatch log group for ECS
resource "aws_cloudwatch_log_group" "payment_app" {
  name              = "/ecs/payment-app"
  retention_in_days = 7
}


# ECS Task Definition
resource "aws_ecs_task_definition" "app_task" {
  family                   = "payment-task"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  network_mode             = "awsvpc"
  execution_role_arn       = data.aws_iam_role.ecs_task_execution_role.arn

  runtime_platform {
    cpu_architecture = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "payment-app"
      image     = var.ecr_repository_url
      essential = true

      portMappings = [
        { containerPort = 8080, hostPort = 8080 }
      ]

      environment = [
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}?connectTimeout=3&socketTimeout=3" },
        { name = "DB_USER", value = var.db_username },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "DB_NAME", value = var.db_name }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/payment-app"
          awslogs-region        = "us-west-2"
          awslogs-stream-prefix = "ecs"
        }
      }
      
    }
  ])
}


# ECS Service
resource "aws_ecs_service" "app_service" {
  name            = "payment-service"
  cluster         = aws_ecs_cluster.app_cluster.id
  task_definition = aws_ecs_task_definition.app_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  depends_on      = [aws_cloudwatch_log_group.payment_app]

  lifecycle {
    ignore_changes = [task_definition]
  }

  network_configuration {
    subnets         = data.aws_subnets.default.ids
    assign_public_ip = true
    security_groups = [aws_security_group.ecs_security_group.id]
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
}